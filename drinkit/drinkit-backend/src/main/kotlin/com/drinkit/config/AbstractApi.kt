package com.drinkit.config

import com.drinkit.security.userdetails.InternalUserDetails
import com.drinkit.user.UserId
import org.springframework.security.core.context.SecurityContextHolder

class ConnectedUserException(message: String): RuntimeException(message)

abstract class AbstractApi {

    fun connectedUserId(): UserId? {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.isAuthenticated)
            return null

        return when (authentication.principal) {
            is InternalUserDetails -> UserId((authentication.principal as InternalUserDetails).id)
            else -> null
        }
    }

    fun connectedUserIdOrFail(): UserId {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.isAuthenticated)
            throw ConnectedUserException("No connected user")

        return when (authentication.principal) {
            is InternalUserDetails -> UserId((authentication.principal as InternalUserDetails).id)
            else -> throw ConnectedUserException("Can't find userId from authentication principal")
        }
    }
}