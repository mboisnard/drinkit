package com.drinkit.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service

class AuthenticationFailedException: RuntimeException()
interface AuthenticationService {

    @Throws(AuthenticationFailedException::class)
    fun authenticate(username: String, password: String)
}

@Service
internal class InternalAuthenticationService(
    private val request: HttpServletRequest?,
    private val response: HttpServletResponse?,
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
): AuthenticationService {

    private val logger = KotlinLogging.logger { }

    @Throws(AuthenticationFailedException::class)
    override fun authenticate(username: String, password: String) {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, password)
            )

            val securityContext = SecurityContextHolder.getContext()
            securityContext.authentication = authentication

            securityContextRepository.saveContext(securityContext, request!!, response!!)
        } catch (ex: AuthenticationException) {
            logger.debug(ex) { "Authentication failed for user $username" }

            throw AuthenticationFailedException()
        }
    }
}