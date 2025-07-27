package com.drinkit.user.registration

import com.drinkit.api.generated.api.RegistrationApiDelegate
import com.drinkit.api.generated.model.CompleteUserInformationRequest
import com.drinkit.api.generated.model.ConfirmEmailRequest
import com.drinkit.api.generated.model.CreateUserRequest
import com.drinkit.common.Author
import com.drinkit.common.CorrelationId
import com.drinkit.config.AbstractApi
import com.drinkit.security.AuthenticationService
import com.drinkit.user.CompleteProfileInformation
import com.drinkit.user.CompleteProfileInformationCommand
import com.drinkit.user.ConfirmVerificationToken
import com.drinkit.user.ConfirmVerificationTokenCommand
import com.drinkit.user.CreateNewUser
import com.drinkit.user.CreateNewUser.Result.UserAlreadyExists
import com.drinkit.user.CreateNewUser.Result.UserCreated
import com.drinkit.user.CreateNewUserCommand
import com.drinkit.user.SendVerificationToken
import com.drinkit.user.SendVerificationTokenCommand
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.Password
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.UserId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
internal class RegistrationApi(
    private val request: HttpServletRequest,

    private val createANewUser: CreateNewUser,
    private val sendVerificationToken: SendVerificationToken,
    private val confirmVerificationToken: ConfirmVerificationToken,
    private val completeProfileInformation: CompleteProfileInformation,

    private val passwordEncoder: PasswordEncoder,
    private val authenticationService: AuthenticationService,
) : RegistrationApiDelegate, AbstractApi() {

    override fun createNewUser(createUserRequest: CreateUserRequest): ResponseEntity<UserId> {

        require(createUserRequest.password == createUserRequest.confirmedPassword) {
            "Password and Password confirmation are not equals"
        }

        val password = Password(createUserRequest.password)

        val result = createANewUser.invoke(
            CreateNewUserCommand(
                author = Author.Unlogged(CorrelationId.create()),
                email = Email(createUserRequest.email),
                password = EncodedPassword.from(password, passwordEncoder::encode),
                locale = request.locale,
            )
        )

        return when (result) {
            is UserCreated -> {
                authenticationService.authenticate(createUserRequest.email, createUserRequest.password)
                ResponseEntity.status(HttpStatus.CREATED).body(result.user.id)
            }
            UserAlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    override fun confirmUserEmail(confirmEmailRequest: ConfirmEmailRequest): ResponseEntity<Unit> {
        val userId = connectedUserIdOrFail()
        val result = confirmVerificationToken.invoke(
            userId = userId,
            command = ConfirmVerificationTokenCommand(
                author = Author.Connected(userId),
                token = confirmEmailRequest.validationToken,
            )
        )

        return when (result) {
            is ConfirmVerificationToken.Result.Success -> ResponseEntity.noContent().build()
            ConfirmVerificationToken.Result.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            ConfirmVerificationToken.Result.NotFound -> ResponseEntity.notFound().build()
            ConfirmVerificationToken.Result.TokenExpired -> ResponseEntity.status(HttpStatus.GONE).build()
        }
    }

    override fun resendValidationToken(): ResponseEntity<Unit> {
        val userId = connectedUserIdOrFail()

        val result = sendVerificationToken.invoke(
            userId = userId,
            command = SendVerificationTokenCommand(
                author = Author.Connected(userId),
                locale = request.locale,
            )
        )

        return when (result) {
            is SendVerificationToken.Result.Success -> ResponseEntity.ok().build()
            SendVerificationToken.Result.AlreadyVerified -> ResponseEntity.status(HttpStatus.GONE).build()
            SendVerificationToken.Result.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            SendVerificationToken.Result.UserNotFound -> ResponseEntity.notFound().build()
        }
    }

    override fun completeUserInformation(
        completeUserInformationRequest: CompleteUserInformationRequest
    ): ResponseEntity<Unit> {
        val userId = connectedUserIdOrFail()
        val command = with(completeUserInformationRequest) {
            CompleteProfileInformationCommand(
                author = Author.Connected(userId),
                profileInformation = ProfileInformation(
                    firstName = FirstName(firstname),
                    lastName = LastName(lastname),
                    birthDate = BirthDate(birthdate),
                )
            )
        }

        val result = completeProfileInformation.invoke(userId, command)

        return when (result) {
            is CompleteProfileInformation.Result.Success -> {
                authenticationService.refreshContext()
                ResponseEntity.ok().build()
            }
            CompleteProfileInformation.Result.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            CompleteProfileInformation.Result.UserNotFound -> ResponseEntity.notFound().build()
        }
    }
}
