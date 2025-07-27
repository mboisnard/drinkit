package com.drinkit.user.spi

import com.drinkit.user.UserFixtures
import com.drinkit.user.core.UserHistory
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class UserEventsTestContract {

    abstract fun fetchUsers(): Users
    abstract fun fetchUserEvents(users: Users): UserEvents

    private val userFixtures = UserFixtures()
    private val users: Users by lazy { fetchUsers() }
    private val userEvents: UserEvents by lazy { fetchUserEvents(users) }

    @Test
    fun `should save a user history starting by an initialized event`() {
        // Given
        val userInitialized = userFixtures.givenAUserHistory().initEvent

        // When
        userEvents.save(userInitialized)

        // Then
        userEvents.findAllBy(userInitialized.userId).shouldNotBeNull() shouldBe UserHistory(userInitialized)

        val savedUser = users.findEnabledBy(userInitialized.userId)
        savedUser.shouldNotBeNull() should {
            it.email shouldBe userInitialized.email
            it.password shouldBe userInitialized.password
            it.roles shouldBe userInitialized.roles
        }
    }
}