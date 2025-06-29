package com.drinkit.user.spi

class InMemoryUsersRepositoryTest: UsersTestContract() {

    override fun fetchRepository(): Users = InMemoryUsersRepository()
}