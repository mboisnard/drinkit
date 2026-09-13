package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.drinkit.security.userdetails.InternalUserDetailsService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service

class AuthenticationFailedException : RuntimeException()

@TechStarterTool
interface AuthenticationService {

    @Throws(AuthenticationFailedException::class)
    fun authenticate(username: String, password: String)

    fun refreshContext()
}

@Service
internal class InternalAuthenticationService(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
    private val userDetailsService: InternalUserDetailsService,
    private val securityContextHolderStrategy: SecurityContextHolderStrategy,
) : AuthenticationService {

    private val logger = KotlinLogging.logger { }

    @Throws(AuthenticationFailedException::class)
    override fun authenticate(username: String, password: String) {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, password),
            )

            val securityContext = securityContextHolderStrategy.createEmptyContext()
            securityContext.authentication = authentication

            securityContextHolderStrategy.context = securityContext
            securityContextRepository.saveContext(securityContext, request, response)
        } catch (ex: AuthenticationException) {
            logger.debug(ex) { "Authentication failed for user $username" }

            throw AuthenticationFailedException()
        }
    }

    override fun refreshContext() {
        // Spring Security 7 annotates this as nullable; refreshing without an authentication
        // has never been valid, so fail explicitly rather than on a NullPointerException
        val oldAuthentication = requireNotNull(securityContextHolderStrategy.context.authentication) {
            "Cannot refresh the security context: no authentication in the current context"
        }

        val recentlyUpdatedUser = userDetailsService.loadUserByUsername(oldAuthentication.name)

        val securityContext = securityContextHolderStrategy.createEmptyContext()
        securityContext.authentication = PreAuthenticatedAuthenticationToken(
            requireNotNull(oldAuthentication.principal) { "Authentication has no principal" },
            oldAuthentication.credentials,
            recentlyUpdatedUser.authorities,
        )

        securityContextHolderStrategy.context = securityContext
        securityContextRepository.saveContext(securityContext, request, response)
    }
}
