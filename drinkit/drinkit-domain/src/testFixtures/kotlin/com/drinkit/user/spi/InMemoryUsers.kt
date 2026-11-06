package com.drinkit.user.spi

import com.drinkit.user.core.Email
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserStatus

class InMemoryUsers : Users {

    private val users: MutableMap<UserId, User> = mutableMapOf()

    override fun saveOrUpdate(user: User): User {
        users[user.id] = user
        return user
    }

    override fun findEnabledBy(userId: UserId): User? = users[userId]?.takeIf { it.enabled }

    override fun findEnabledBy(email: Email): User? = users.values.firstOrNull { it.email == email && it.enabled }

    override fun exists(email: Email): Boolean = users.values.any { it.email == email }

    override fun findDeletedBy(email: Email): User? =
        users.values.firstOrNull { it.email == email && it.status == UserStatus.DELETED }

    fun count(): Int = users.size
}
