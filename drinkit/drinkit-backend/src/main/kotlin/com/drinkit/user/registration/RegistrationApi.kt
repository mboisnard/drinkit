package com.drinkit.user.registration

import com.drinkit.api.generated.api.RegistrationApiDelegate
import com.drinkit.api.generated.model.CompleteUserInformationRequest
import com.drinkit.api.generated.model.ConfirmEmailRequest
import com.drinkit.api.generated.model.CreateUserRequest
import com.drinkit.config.AbstractApi
import com.drinkit.security.AuthenticationService
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.Email
import com.drinkit.user.EncodedPassword
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.Password
import com.drinkit.user.core.UserId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
internal class RegistrationApi(
    private val request: HttpServletRequest?,
    private val createANotCompletedUser: CreateANotCompletedUser,
    private val validateEmail: ValidateEmail,
    private val completeUserInformation: CompleteUserInformation,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationService: AuthenticationService,
) : RegistrationApiDelegate, AbstractApi() {

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
                locale = request!!.locale
            )
        )

        authenticationService.authenticate(createUserRequest.email, createUserRequest.password)

        return ResponseEntity.status(HttpStatus.CREATED).body(userId)
    }

    override fun confirmUserEmail(confirmEmailRequest: ConfirmEmailRequest): ResponseEntity<Unit> {
        validateEmail.validateVerificationToken(
            userId = connectedUserIdOrFail(),
            token = confirmEmailRequest.validationToken
        )

        return ResponseEntity.ok().build()
    }

    override fun resendValidationToken(): ResponseEntity<Unit> {
        return super.resendValidationToken()
    }

    override fun completeUserInformation(
        completeUserInformationRequest: CompleteUserInformationRequest
    ): ResponseEntity<Unit> {
        val command = with(completeUserInformationRequest) {
            CompleteUserInformationCommand(
                userId = connectedUserIdOrFail(),
                firstName = FirstName(firstname),
                lastName = LastName(lastname),
                birthDate = BirthDate(birthdate),
            )
        }

        completeUserInformation(command)
        authenticationService.refreshContext()

        return ResponseEntity.ok().build()
    }
}
