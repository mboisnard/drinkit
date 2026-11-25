package com.drinkit.user

import com.drinkit.common.Constants.DELETED_ACCOUNT_RETENTION
import com.drinkit.i18n.FixedCurrentLocale
import com.drinkit.security.InMemoryCurrentVisitor
import com.drinkit.security.UserAccount
import com.drinkit.security.UserAccounts
import com.drinkit.security.UserAccountsTestContract
import com.drinkit.user.core.Email
import com.drinkit.user.core.Preferences
import com.drinkit.user.core.UserStatus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Locale

internal class DomainUserAccountsTest : UserAccountsTestContract() {

    private val userFixtures = UserFixtures()
    private val clock = userFixtures.controlledClock.fix()
    private val currentLocale = FixedCurrentLocale(Locale.ENGLISH)

    override fun fetchUserAccounts(): UserAccounts = DomainUserAccounts(
        users = userFixtures.users,
        createNewUser = userFixtures.createNewUser,
        reactivateUser = userFixtures.reactivateUser,
        currentVisitor = InMemoryCurrentVisitor(),
        currentLocale = currentLocale,
    )

    override fun givenAnAccount(login: String): UserAccount {
        val user = userFixtures.givenAnInitializedUser(email = Email(login))
        return UserAccount(id = user.id.value, login = login, roles = setOf("ROLE_REGISTRATION_IN_PROGRESS"))
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

    @Test
    fun `registers a new user, who has a profile to complete`() {
        // When
        val account = accounts.register("grace@drinkit.test").shouldNotBeNull()

        // Then
        account.roles shouldBe setOf("ROLE_REGISTRATION_IN_PROGRESS")
        userFixtures.users.findEnabledBy(Email("grace@drinkit.test")).shouldNotBeNull().status shouldBe
            UserStatus.PROFILE_COMPLETION_REQUIRED
    }

    @Test
    fun `registers a new user in the language the request reads in`() {
        // When
        accounts.register("grace@drinkit.test")

        // Then
        userFixtures.users.findEnabledBy(Email("grace@drinkit.test")).shouldNotBeNull().preferences shouldBe
            Preferences(language = Locale.ENGLISH)
    }

    @Test
    fun `keeps the language of the account for the session it signs in, whatever the request reads in`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()

        // When
        accounts.signedIn(user.id.value)

        // Then
        currentLocale.get() shouldBe user.preferences.language
    }

    @Test
    fun `brings back the user of an email deleted within the retention period`() {
        // Given
        val user = givenAUserDeleted(ago = Duration.ofDays(1))

        // When
        val account = accounts.register(user.email.value)

        // Then
        account.shouldNotBeNull().id shouldBe user.id.value
    }

    @Test
    fun `refuses the email of a user deleted longer ago than the retention period`() {
        // Given
        val user = givenAUserDeleted(ago = DELETED_ACCOUNT_RETENTION)

        // When
        val account = accounts.register(user.email.value)

        // Then
        account shouldBe null
    }

    @Test
    fun `refuses a login that is not an email`() {
        accounts.register("not an email") shouldBe null
    }

    private fun givenAUserDeleted(ago: Duration) = userFixtures.givenAnInitializedUser().also { user ->
        clock.add(ago.negated())
        userFixtures.deleteUser.invoke(user.id, DeleteUserCommand(author = user.id.toConnectedAuthor()))
        clock.add(ago)
    }
}
