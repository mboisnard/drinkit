package com.drinkit.user.registration.api

import com.drinkit.api.generated.api.RegistrationApiDelegate
import com.drinkit.api.generated.model.CompleteUserInformationRequest
import com.drinkit.api.generated.model.ConfirmEmailRequest
import com.drinkit.api.generated.model.CreateUserRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
internal class RegistrationApi(

): RegistrationApiDelegate {

    override fun createNewUser(createUserRequest: CreateUserRequest): ResponseEntity<Unit> {
        return super.createNewUser(createUserRequest)
    }

    override fun confirmUserEmail(confirmEmailRequest: ConfirmEmailRequest): ResponseEntity<Unit> {
        return super.confirmUserEmail(confirmEmailRequest)
    }

    override fun completeUserInformation(completeUserInformationRequest: CompleteUserInformationRequest): ResponseEntity<Unit> {
        return super.completeUserInformation(completeUserInformationRequest)
    }
}