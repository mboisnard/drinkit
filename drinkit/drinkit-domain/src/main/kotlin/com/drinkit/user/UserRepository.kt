package com.drinkit.user

interface UserRepository {

    fun findById(userId: UserId): User?
}