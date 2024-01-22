package com.drinkit.user

import com.drinkit.api.generated.api.AuthApiDelegate
import com.drinkit.api.generated.model.SignInRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import org.springframework.stereotype.Service

@Service
internal class AuthApi(
    private val request: HttpServletRequest?,
    private val authenticationManager: AuthenticationManager,
): AuthApiDelegate {

    override fun authenticateUser(signInRequest: SignInRequest): ResponseEntity<Unit> {

        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(signInRequest.username, signInRequest.password)
            )
            val securityContext = SecurityContextHolder.getContext()
            securityContext.authentication = authentication

            val session = request!!.getSession(true)
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext)

            return ResponseEntity.ok().build()
        } catch (ex: AuthenticationException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}