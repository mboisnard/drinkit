package com.drinkit.user

import com.drinkit.api.generated.api.UserApiDelegate
import com.drinkit.api.generated.model.ConnectedUserInformation
import com.drinkit.config.AbstractApi
import com.drinkit.user.spi.Users
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
internal class UserApi(
    private val users: Users
) : UserApiDelegate, AbstractApi() {

    override fun getConnectedUserInfo(): ResponseEntity<ConnectedUserInformation> {

        users.findEnabledBy(connectedUserIdOrFail())

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
