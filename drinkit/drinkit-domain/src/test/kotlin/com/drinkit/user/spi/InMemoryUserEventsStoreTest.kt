package com.drinkit.user.spi

internal class InMemoryUserEventsStoreTest: UserEventsTestContract() {
    override fun fetchUsers(): Users = InMemoryUsersRepository()
    override fun fetchUserEvents(users: Users): UserEvents = InMemoryUserEventsStore(users)
}