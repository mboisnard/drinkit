package com.drinkit.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

internal abstract class CurrentUserTestContract {

    protected val accounts = InMemoryUserAccounts()

    abstract val currentUser: CurrentUser

    @Test
    fun `nobody is signed in until a user signs in`() {
        // When & Then
        currentUser.orNull().shouldBeNull()
        currentUser.refresh().shouldBeNull()
        shouldThrow<AuthenticationCredentialsNotFoundException> { currentUser.get() }
    }

    @Test
    fun `a user signs in with the roles of its account`() {
        // Given
        accounts.save("ada@drinkit.com", UserAccount("ada", setOf("ROLE_USER")))

        // When
        val signedIn = currentUser.signIn("ada")

        // Then
        signedIn shouldBe ConnectedUser("ada", setOf("ROLE_USER"))
        currentUser.get() shouldBe signedIn
    }

    @Test
    fun `a user without an account cannot sign in`() {
        // When
        shouldThrow<IllegalArgumentException> { currentUser.signIn("nobody") }

        // Then
        currentUser.orNull().shouldBeNull()
    }

    @Test
    fun `a refresh reloads the roles of the account`() {
        // Given
        accounts.save("ada@drinkit.com", UserAccount("ada", setOf("ROLE_USER")))
        currentUser.signIn("ada")
        accounts.save("ada@drinkit.com", UserAccount("ada", setOf("ROLE_USER", "ROLE_ADMIN")))

        // When
        val refreshed = currentUser.refresh()

        // Then
        refreshed shouldBe ConnectedUser("ada", setOf("ROLE_USER", "ROLE_ADMIN"))
        currentUser.get() shouldBe refreshed
    }

    @Test
    fun `a refresh signs out a user whose account is gone`() {
        // Given
        accounts.save("ada@drinkit.com", UserAccount("ada", setOf("ROLE_USER")))
        currentUser.signIn("ada")
        accounts.remove("ada")

        // When
        val refreshed = currentUser.refresh()

        // Then
        refreshed.shouldBeNull()
        currentUser.orNull().shouldBeNull()
    }
}
