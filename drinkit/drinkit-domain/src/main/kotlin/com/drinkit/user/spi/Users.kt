package com.drinkit.user.spi

import com.drinkit.user.core.Email
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId

interface Users {

    fun saveOrUpdate(user: User): User

    fun findBy(userId: UserId): User?

    fun exists(email: Email): Boolean
}