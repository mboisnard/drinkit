package com.drinkit.config

import com.drinkit.security.userdetails.InternalUserDetails
import com.drinkit.user.CompletedUser
import com.drinkit.user.UserId
import com.drinkit.user.CompletedUsers
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component


@Component
//@Scope("session", proxyMode = ScopedProxyMode.TARGET_CLASS)
class ConnectedUser(
    private val completedUsers: CompletedUsers,
) {
    //private var user: User? = null

    fun getOrFail(): CompletedUser {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.isAuthenticated)
            throw ConnectedUserException("No connected user")

        val userId = when (authentication.principal) {
            is InternalUserDetails -> UserId((authentication.principal as InternalUserDetails).id)
            else -> throw ConnectedUserException("Can't find userId from authentication principal")
        }

        //if (user == null)
        //    user = userRepository.findById(userId)

        return completedUsers.findById(userId) ?: throw ConnectedUserException("User not found from id $userId")
    }
}