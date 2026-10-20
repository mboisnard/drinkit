package com.drinkit.user

import com.drinkit.api.generated.api.UserApi
import com.drinkit.api.generated.model.ConnectedUserInformation
import com.drinkit.config.AbstractApi
import com.drinkit.user.spi.Users
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@RestController
internal class UserController(private val users: Users) :
    AbstractApi(),
    UserApi {

    @Transactional(readOnly = true)
    override fun getConnectedUserInfo(): ResponseEntity<ConnectedUserInformation> {
        users.findEnabledBy(connectedUserIdOrFail())

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
