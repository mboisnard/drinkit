package com.drinkit.user.registration

internal class InMemoryNotCompletedUsersTest : NotCompletedCompletedUsersTestContract() {

    override fun fetchRepository(): NotCompletedUsers =
        InMemoryNotCompletedUsers()
}