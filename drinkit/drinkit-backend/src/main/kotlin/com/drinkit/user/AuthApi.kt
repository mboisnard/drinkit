package com.drinkit.user

import com.drinkit.api.generated.api.AuthApiDelegate
import com.drinkit.api.generated.model.SignInRequest
import com.drinkit.security.AuthenticationFailedException
import com.drinkit.security.AuthenticationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
internal class AuthApi(
    private val authenticationService: AuthenticationService,
) : AuthApiDelegate {

    override fun authenticateUser(signInRequest: SignInRequest): ResponseEntity<Unit> {
        try {
            authenticationService.authenticate(signInRequest.username, signInRequest.password)
            return ResponseEntity.ok().build()
        } catch (ex: AuthenticationFailedException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
