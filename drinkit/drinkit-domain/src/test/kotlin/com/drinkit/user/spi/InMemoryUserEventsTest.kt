package com.drinkit.user.spi

internal class InMemoryUserEventsTest : UserEventsTestContract() {
    override fun fetchUsers(): Users = InMemoryUsers()
    override fun fetchUserEvents(users: Users): UserEvents = InMemoryUserEvents(users)
}
