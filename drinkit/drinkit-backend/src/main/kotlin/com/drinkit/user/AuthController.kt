package com.drinkit.user

import com.drinkit.api.generated.api.AuthApi
import com.drinkit.api.generated.model.SignInRequest
import com.drinkit.security.AuthenticationFailedException
import com.drinkit.security.AuthenticationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
internal class AuthController(private val authenticationService: AuthenticationService) : AuthApi {

    override fun authenticateUser(signInRequest: SignInRequest): ResponseEntity<Unit> {
        try {
            authenticationService.authenticate(signInRequest.username, signInRequest.password)
            return ResponseEntity.ok().build()
        } catch (_: AuthenticationFailedException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
