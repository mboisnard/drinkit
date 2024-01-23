package com.drinkit.user

import com.drinkit.api.generated.api.AuthApiDelegate
import com.drinkit.api.generated.model.SignInRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service

@Service
internal class AuthApi(
    private val request: HttpServletRequest?,
    private val response: HttpServletResponse?,
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
): AuthApiDelegate {

    override fun authenticateUser(signInRequest: SignInRequest): ResponseEntity<Unit> {

        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(signInRequest.username, signInRequest.password)
            )
            val securityContext = SecurityContextHolder.getContext()
            securityContext.authentication = authentication

            securityContextRepository.saveContext(securityContext, request!!, response!!)

            return ResponseEntity.ok().build()
        } catch (ex: AuthenticationException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}