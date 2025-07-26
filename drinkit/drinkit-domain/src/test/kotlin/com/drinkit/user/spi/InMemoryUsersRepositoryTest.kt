package com.drinkit.user.spi

internal class InMemoryUsersRepositoryTest: UsersTestContract() {

    override fun fetchRepository(): Users = InMemoryUsersRepository()
}