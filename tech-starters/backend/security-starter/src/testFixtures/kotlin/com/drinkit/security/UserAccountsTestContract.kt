package com.drinkit.security

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val LOGIN = "ada@drinkit.test"

/** The behavior every [UserAccounts] keeps: run it against the application's adapter. */
abstract class UserAccountsTestContract {

    protected val accounts: UserAccounts by lazy { fetchUserAccounts() }

    abstract fun fetchUserAccounts(): UserAccounts

    /** Stores an enabled account of [login] by the application's own means, returned as [UserAccounts] reads it. */
    abstract fun givenAnAccount(login: String): UserAccount

    @Test
    fun `finds no account for an unknown login`() {
        accounts.findByLogin("nobody@drinkit.test") shouldBe null
    }

    @Test
    fun `finds no account for an unknown id`() {
        accounts.findById("unknown-id") shouldBe null
    }

    @Test
    fun `finds by id the account it finds by login`() {
        // Given
        val account = givenAnAccount(LOGIN)

        // When
        val byId = accounts.findById(account.id)

        // Then
        byId.shouldNotBeNull() shouldBe accounts.findByLogin(LOGIN)
    }
}
