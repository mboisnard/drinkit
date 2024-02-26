package com.drinkit.user.registration

internal class InMemoryNotCompletedCompletedUsersTest: NotCompletedCompletedUsersContract() {

    override fun fetchRepository(): NotCompletedUsers =
        InMemoryNotCompletedUsers()
}