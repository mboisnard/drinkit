package com.drinkit.user.registration

import com.drinkit.user.Email
import com.drinkit.user.NotCompletedUser
import com.drinkit.user.UserId

class InMemoryNotCompletedUsers: NotCompletedUsers {

    private val users: MutableMap<UserId, NotCompletedUser> = mutableMapOf()

    override fun emailExists(email: Email): Boolean =
        users.values.any { it.email == email }

    override fun findById(userId: UserId): NotCompletedUser? =
        users[userId]

    override fun create(user: NotCompletedUser): UserId? {
        if (users.containsKey(user.id))
            return null

        users[user.id] = user

        return user.id
    }

    override fun update(user: NotCompletedUser) {
        if (!users.containsKey(user.id))
            return

        users[user.id] = user
    }
}