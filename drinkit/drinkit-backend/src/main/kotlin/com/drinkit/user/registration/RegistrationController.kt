package com.drinkit.user.registration

import com.drinkit.api.generated.api.RegistrationApi
import com.drinkit.api.generated.model.CompleteUserInformationRequest
import com.drinkit.security.CurrentUser
import com.drinkit.user.CompleteProfileInformation
import com.drinkit.user.CompleteProfileInformationCommand
import com.drinkit.user.author
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.userId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
internal class RegistrationController(
    private val completeProfileInformation: CompleteProfileInformation,
    private val currentUser: CurrentUser,
) : RegistrationApi {

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
