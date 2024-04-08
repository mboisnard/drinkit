package com.drinkit.user.registration

import com.drinkit.user.NotCompletedUsers

internal class InMemoryNotCompletedUsersTest : NotCompletedCompletedUsersTestContract() {

    override fun fetchRepository(): NotCompletedUsers =
        InMemoryNotCompletedUsers()
}
