package com.drinkit.user.spi

import com.drinkit.user.core.Email
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import org.springframework.stereotype.Repository

// TODO Should be in testFixtures after implementing the Jooq Repository
@Repository
class InMemoryUsersRepository: Users {

    private val users: MutableMap<UserId, User> = mutableMapOf()

    override fun saveOrUpdate(user: User): User {
        users[user.id] = user
        return user
    }

    override fun findBy(userId: UserId): User? = users[userId]

    override fun exists(email: Email): Boolean = users.values.any { it.email == email }

    fun count(): Int = users.size
}