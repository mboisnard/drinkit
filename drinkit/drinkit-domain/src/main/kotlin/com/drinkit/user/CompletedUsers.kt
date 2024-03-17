package com.drinkit.user

interface CompletedUsers {

    fun findById(userId: UserId): CompletedUser?
}
