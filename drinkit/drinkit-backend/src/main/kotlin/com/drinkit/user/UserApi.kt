package com.drinkit.user

import com.drinkit.api.generated.api.UserApiDelegate
import com.drinkit.api.generated.model.ConnectedUserInformation
import com.drinkit.api.generated.model.Role
import com.drinkit.config.AbstractApi
import com.drinkit.user.registration.NotCompletedUsers
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
internal class UserApi(
    private val completedUsers: CompletedUsers,
    private val notCompletedUsers: NotCompletedUsers,
) : UserApiDelegate, AbstractApi() {

    // TODO Should be cleaned and simplified
    override fun getConnectedUserInfo(): ResponseEntity<ConnectedUserInformation> {
        val connectedUserId = connectedUserIdOrFail()

        val completedUserInformation = completedUsers.findById(connectedUserId)
            ?.let {
                ConnectedUserInformation(
                    firstname = it.firstname.value,
                    lastname = it.lastName.value,
                    birthdate = it.birthDate?.value,
                    roles = it.roles.toRoleResponse()
                )
            }

        if (completedUserInformation != null) {
            return ResponseEntity.ok(completedUserInformation)
        }

        val notCompletedUserInformation = notCompletedUsers.findById(connectedUserId)
            ?.let {
                ConnectedUserInformation(
                    firstname = it.firstname?.value,
                    lastname = it.lastName?.value,
                    birthdate = it.birthDate?.value,
                    roles = it.roles?.toRoleResponse() ?: emptyList()
                )
            }

        return notCompletedUserInformation
            ?.let { ResponseEntity.ok(notCompletedUserInformation) }
            ?: ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    private fun Roles.toRoleResponse() = this.values.map { role ->
        when (role) {
            Roles.Role.ROLE_USER -> Role.ROLE_USER
            Roles.Role.ROLE_ADMIN -> Role.ROLE_ADMIN
        }
    }
}
