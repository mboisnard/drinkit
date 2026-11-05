package com.drinkit.user.registration

import com.drinkit.api.generated.api.RegistrationApi
import com.drinkit.api.generated.model.CompleteUserInformationRequest
import com.drinkit.api.generated.model.ConfirmEmailRequest
import com.drinkit.api.generated.model.CreateUserRequest
import com.drinkit.common.CorrelationId
import com.drinkit.security.CurrentUser
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
import com.drinkit.user.author
import com.drinkit.user.core.Author
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.Password
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.UserId
import com.drinkit.user.userId
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.RestController

@RestController
internal class RegistrationController(
    private val createANewUser: CreateNewUser,
    private val sendVerificationToken: SendVerificationToken,
    private val confirmVerificationToken: ConfirmVerificationToken,
    private val completeProfileInformation: CompleteProfileInformation,

    private val passwordEncoder: PasswordEncoder,
    private val currentUser: CurrentUser,
) : RegistrationApi {

    override fun createNewUser(createUserRequest: CreateUserRequest): ResponseEntity<UserId> {
        require(createUserRequest.password == createUserRequest.confirmedPassword) {
            "Password and Password confirmation are not equals"
        }

        val password = Password(createUserRequest.password)

        val result = createANewUser.invoke(
            CreateNewUserCommand(
                author = Author.Unlogged(CorrelationId.create()),
                email = Email(createUserRequest.email),
                password = EncodedPassword.from(password) { raw ->
                    requireNotNull(passwordEncoder.encode(raw)) { "Password encoder returned no value" }
                },
                locale = LocaleContextHolder.getLocale(),
            ),
        )

        return when (result) {
            is UserCreated -> {
                currentUser.signIn(result.user.id.value)
                ResponseEntity.status(HttpStatus.CREATED).body(result.user.id)
            }

            UserAlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    override fun confirmUserEmail(confirmEmailRequest: ConfirmEmailRequest): ResponseEntity<Unit> {
        val connectedUser = currentUser.get()
        val result = confirmVerificationToken.invoke(
            userId = connectedUser.userId,
            command = ConfirmVerificationTokenCommand(
                author = connectedUser.author,
                token = confirmEmailRequest.validationToken,
            ),
        )

        return when (result) {
            is ConfirmVerificationToken.Result.Success -> ResponseEntity.noContent().build()
            ConfirmVerificationToken.Result.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            ConfirmVerificationToken.Result.NotFound -> ResponseEntity.notFound().build()
            ConfirmVerificationToken.Result.TokenExpired -> ResponseEntity.status(HttpStatus.GONE).build()
        }
    }

    override fun resendValidationToken(): ResponseEntity<Unit> {
        val connectedUser = currentUser.get()

        val result = sendVerificationToken.invoke(
            userId = connectedUser.userId,
            command = SendVerificationTokenCommand(
                author = connectedUser.author,
                locale = LocaleContextHolder.getLocale(),
            ),
        )

        return when (result) {
            is SendVerificationToken.Result.Success -> ResponseEntity.ok().build()
            SendVerificationToken.Result.AlreadyVerified -> ResponseEntity.status(HttpStatus.GONE).build()
            SendVerificationToken.Result.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            SendVerificationToken.Result.UserNotFound -> ResponseEntity.notFound().build()
        }
    }

    override fun completeUserInformation(
        completeUserInformationRequest: CompleteUserInformationRequest,
    ): ResponseEntity<Unit> {
        val connectedUser = currentUser.get()
        val command = with(completeUserInformationRequest) {
            CompleteProfileInformationCommand(
                author = connectedUser.author,
                profileInformation = ProfileInformation(
                    firstName = FirstName(firstname),
                    lastName = LastName(lastname),
                    birthDate = BirthDate(birthdate),
                ),
            )
        }

        val result = completeProfileInformation.invoke(connectedUser.userId, command)

        return when (result) {
            is CompleteProfileInformation.Result.Success -> {
                currentUser.refresh()
                ResponseEntity.ok().build()
            }

            CompleteProfileInformation.Result.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN).build()

            CompleteProfileInformation.Result.UserNotFound -> ResponseEntity.notFound().build()
        }
    }
}
