package com.drinkit.user

interface Users {

    fun findById(userId: UserId): User?
}