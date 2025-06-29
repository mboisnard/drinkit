package com.drinkit.user.spi

import com.drinkit.user.core.User
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId

interface UserEvents {

    fun findAllBy(userId: UserId): UserHistory?

    fun save(event: UserEvent): User
}