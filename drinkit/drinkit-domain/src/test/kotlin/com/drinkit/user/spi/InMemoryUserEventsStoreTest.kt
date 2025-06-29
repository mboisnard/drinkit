package com.drinkit.user.spi

class InMemoryUserEventsStoreTest: UserEventsTestContract() {
    override fun fetchUsers(): Users = InMemoryUsersRepository()
    override fun fetchUserEvents(users: Users): UserEvents = InMemoryUserEventsStore(users)
}