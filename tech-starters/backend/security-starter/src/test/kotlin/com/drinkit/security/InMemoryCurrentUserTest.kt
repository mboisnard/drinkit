package com.drinkit.security

internal class InMemoryCurrentUserTest : CurrentUserTestContract() {

    override val currentUser = InMemoryCurrentUser(accounts)
}
