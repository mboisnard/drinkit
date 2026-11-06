package com.drinkit.security

internal class InMemoryUserAccountsTest : UserAccountsTestContract() {

    private val inMemory = InMemoryUserAccounts()

    override fun fetchUserAccounts(): UserAccounts = inMemory

    override fun givenAnAccount(login: String): UserAccount =
        UserAccount("user-1", setOf("ROLE_USER")).also { inMemory.save(login, it) }
}
