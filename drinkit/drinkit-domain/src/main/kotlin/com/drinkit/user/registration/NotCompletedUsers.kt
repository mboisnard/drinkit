package com.drinkit.user.registration

import com.drinkit.user.Email
import com.drinkit.user.NotCompletedUser
import com.drinkit.user.UserId

interface NotCompletedUsers {

    fun emailExists(email: Email): Boolean

    fun findById(userId: UserId): NotCompletedUser?

    fun create(user: NotCompletedUser): UserId?

    fun update(user: NotCompletedUser)
}
