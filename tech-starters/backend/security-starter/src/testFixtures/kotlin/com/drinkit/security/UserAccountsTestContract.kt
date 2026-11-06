package com.drinkit.security

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

private const val LOGIN = "ada@drinkit.test"
private const val NEW_LOGIN = "grace@drinkit.test"

abstract class UserAccountsTestContract {

    protected val accounts: UserAccounts by lazy { fetchUserAccounts() }

    abstract fun fetchUserAccounts(): UserAccounts

    /** An enabled account of [login], stored by the application's own means. */
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

    @Test
    fun `finds by login the account it registers for an unknown login`() {
        // When
        val registered = accounts.register(NEW_LOGIN, Locale.FRENCH)

        // Then
        registered.shouldNotBeNull() shouldBe accounts.findByLogin(NEW_LOGIN)
    }
}
