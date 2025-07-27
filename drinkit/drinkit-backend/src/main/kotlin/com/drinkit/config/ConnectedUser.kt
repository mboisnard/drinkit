package com.drinkit.config

import com.drinkit.security.userdetails.InternalUserDetails
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

// @Scope("session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
class ConnectedUser(
) {
    // private var user: User? = null

    fun getOrFail(): User {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.isAuthenticated) {
            throw ConnectedUserException("No connected user")
        }

        val userId = when (authentication.principal) {
            is InternalUserDetails -> UserId((authentication.principal as InternalUserDetails).id)
            else -> throw ConnectedUserException("Can't find userId from authentication principal")
        }

        // if (user == null)
        //    user = userRepository.findById(userId)

        return TODO()
    }
}
