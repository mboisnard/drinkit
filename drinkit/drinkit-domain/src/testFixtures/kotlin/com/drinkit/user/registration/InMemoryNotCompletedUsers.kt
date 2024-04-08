package com.drinkit.user.registration

import com.drinkit.user.Email
import com.drinkit.user.NotCompletedUser
import com.drinkit.user.NotCompletedUsers
import com.drinkit.user.UserId

class InMemoryNotCompletedUsers : NotCompletedUsers {

    private val users: MutableMap<UserId, NotCompletedUser> = mutableMapOf()

    override fun emailExists(email: Email): Boolean =
        users.values.any { it.email == email }

    override fun findById(userId: UserId): NotCompletedUser? =
        users[userId]

    override fun create(user: NotCompletedUser): UserId? {
        if (users.containsKey(user.id)) {
            return null
        }

        // Reset some fields here to have the same behavior as the production code
        // The `create` method only save mandatory information
        val sanitizedUserToCreate = user.copy(
            firstName = null,
            lastName = null,
            birthDate = null,
            lastConnection = null,
            roles = null,
        )
        users[user.id] = sanitizedUserToCreate

        return user.id
    }

    override fun update(user: NotCompletedUser): Int {
        if (!users.containsKey(user.id)) {
            return 0
        }

        users[user.id] = user
        return 1
    }
}
