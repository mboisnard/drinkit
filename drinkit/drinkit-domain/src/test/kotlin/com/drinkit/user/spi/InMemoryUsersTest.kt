package com.drinkit.user.spi

internal class InMemoryUsersTest : UsersTestContract() {

    override fun fetchRepository(): Users = InMemoryUsers()
}
