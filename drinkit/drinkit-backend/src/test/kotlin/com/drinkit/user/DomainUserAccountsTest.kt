package com.drinkit.user

import com.drinkit.security.UserAccount
import com.drinkit.security.UserAccounts
import com.drinkit.security.UserAccountsTestContract
import com.drinkit.user.core.Email
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class DomainUserAccountsTest : UserAccountsTestContract() {

    private val userFixtures = UserFixtures()

    override fun fetchUserAccounts(): UserAccounts = DomainUserAccounts(users = userFixtures.users)

    override fun givenAnAccount(login: String): UserAccount {
        val user = userFixtures.givenAnInitializedUser(email = Email(login))
        return UserAccount(
            id = user.id.value,
            passwordHash = user.password.value,
            roles = setOf("ROLE_REGISTRATION_IN_PROGRESS"),
        )
    }

    @Test
    fun `reads the account of a registered user, with his roles as authorities`() {
        // Given
        val account = givenAnAccount("ada@drinkit.test")

        // When
        val found = accounts.findByLogin("ada@drinkit.test")

        // Then
        found shouldBe account
    }

    @Test
    fun `finds no account for a deleted user`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        userFixtures.deleteUser.invoke(user.id, DeleteUserCommand(author = user.id.toConnectedAuthor()))

        // When
        val byLogin = accounts.findByLogin(user.email.value)
        val byId = accounts.findById(user.id.value)

        // Then
        byLogin shouldBe null
        byId shouldBe null
    }

    @Test
    fun `finds no account for a login that is not an email`() {
        accounts.findByLogin("not an email") shouldBe null
    }
}
