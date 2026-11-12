package com.drinkit.user

import com.drinkit.api.generated.api.UserApi
import com.drinkit.api.generated.model.ConnectedUserInformation
import com.drinkit.api.generated.model.Role
import com.drinkit.security.CurrentUser
import com.drinkit.user.core.User
import com.drinkit.user.spi.Users
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
internal class UserController(private val users: Users, private val currentUser: CurrentUser) : UserApi {

    @Transactional(readOnly = true)
    override fun getConnectedUserInfo(): ResponseEntity<ConnectedUserInformation> {
        val user = users.findEnabledBy(currentUser.get().userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        return ResponseEntity.ok(user.toInformation())
    }

    private fun User.toInformation() = ConnectedUserInformation(
        roles = roles.values.map { Role.forValue(it.name) },
        firstname = profile?.firstName?.value,
        lastname = profile?.lastName?.value,
        birthdate = profile?.birthDate?.value,
    )
}
