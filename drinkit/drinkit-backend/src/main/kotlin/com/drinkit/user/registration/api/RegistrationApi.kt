package com.drinkit.user.registration.api

import com.drinkit.api.generated.api.RegistrationApiDelegate
import com.drinkit.api.generated.model.CompleteUserInformationRequest
import com.drinkit.api.generated.model.ConfirmEmailRequest
import com.drinkit.api.generated.model.CreateUserRequest
import com.drinkit.security.AuthenticationService
import com.drinkit.user.Email
import com.drinkit.user.EncodedPassword
import com.drinkit.user.Password
import com.drinkit.user.UserId
import com.drinkit.user.registration.CreateANotCompletedUser
import com.drinkit.user.registration.CreateUserCommand
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
internal class RegistrationApi(
    private val createANotCompletedUser: CreateANotCompletedUser,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationService: AuthenticationService,
): RegistrationApiDelegate {

    override fun createNewUser(createUserRequest: CreateUserRequest): ResponseEntity<UserId> {
        val email = Email(createUserRequest.email)

        require(createUserRequest.password == createUserRequest.confirmedPassword) {
            "Password and Password confirmation are not equals"
        }

        val password = Password(createUserRequest.password)

        val userId = createANotCompletedUser(
            CreateUserCommand(
                email = email,
                password = EncodedPassword.from(password, passwordEncoder::encode),
            )
        )

        authenticationService.authenticate(createUserRequest.email, createUserRequest.password)

        return ResponseEntity.status(HttpStatus.CREATED).body(userId)
    }

    override fun confirmUserEmail(confirmEmailRequest: ConfirmEmailRequest): ResponseEntity<Unit> {
        return super.confirmUserEmail(confirmEmailRequest)
    }

    override fun completeUserInformation(completeUserInformationRequest: CompleteUserInformationRequest): ResponseEntity<Unit> {
        return super.completeUserInformation(completeUserInformationRequest)
    }
}