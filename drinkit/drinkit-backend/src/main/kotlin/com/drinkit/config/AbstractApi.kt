package com.drinkit.config

import com.drinkit.security.userdetails.InternalUserDetails
import com.drinkit.user.core.UserId
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Locale

class ConnectedUserException(message: String) : RuntimeException(message)

abstract class AbstractApi {

    fun maybeConnectedUserId(): UserId? {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.isAuthenticated) {
            return null
        }

        return when (authentication.principal) {
            is InternalUserDetails -> UserId((authentication.principal as InternalUserDetails).id)
            else -> null
        }
    }

    fun connectedUserIdOrFail(): UserId {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.isAuthenticated) {
            throw ConnectedUserException("No connected user")
        }

        return when (authentication.principal) {
            is InternalUserDetails -> UserId((authentication.principal as InternalUserDetails).id)
            else -> throw ConnectedUserException("Can't find userId from authentication principal")
        }
    }

    fun locale(): Locale = Locale.FRENCH
}
