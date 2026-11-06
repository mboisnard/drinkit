package com.drinkit.user.spi

import com.drinkit.user.UserFixtures
import com.drinkit.user.core.Email
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class UsersTestContract {

    private val userFixtures = UserFixtures()
    private val repository: Users by lazy { fetchRepository() }

    abstract fun fetchRepository(): Users

    @Test
    fun `should save a user and find it by his id`() {
        // Given
        val user = userFixtures.givenAUserHistory()
            .let { User.from(it) }

        // When
        repository.saveOrUpdate(user)

        // Then
        val savedUser = repository.findEnabledBy(user.id)

        // Then
        savedUser shouldBe user
    }

    @Test
    fun `should find user by his email`() {
        // Given
        val user = userFixtures.givenAUserHistory()
            .let { User.from(it) }
        repository.saveOrUpdate(user)

        // When
        val isUserExistsInDatabase = repository.exists(user.email)

        // Then
        isUserExistsInDatabase shouldBe true
    }

    @Test
    fun `should be null when user is not found by his id`() {
        // Given
        val userId = userFixtures.generateId.invoke(UserId::class)

        // When
        val user = repository.findEnabledBy(userId)

        // Then
        user shouldBe null
    }

    @Test
    fun `finds an enabled user by his email`() {
        // Given
        val user = userFixtures.givenAUserHistory()
            .let { User.from(it) }
        repository.saveOrUpdate(user)

        // When
        val foundUser = repository.findEnabledBy(user.email)

        // Then
        foundUser shouldBe user
    }

    @Test
    fun `finds no deleted user by his email`() {
        // Given
        val user = userFixtures.givenAUserHistory()
            .let { User.from(it) }
            .copy(status = UserStatus.DELETED)
        repository.saveOrUpdate(user)

        // When
        val foundUser = repository.findEnabledBy(user.email)

        // Then
        foundUser shouldBe null
    }

    @Test
    fun `finds the deleted user of an email`() {
        // Given
        val user = userFixtures.givenAUserHistory()
            .let { User.from(it) }
            .copy(status = UserStatus.DELETED)
        repository.saveOrUpdate(user)

        // When
        val foundUser = repository.findDeletedBy(user.email)

        // Then
        foundUser shouldBe user
    }

    @Test
    fun `finds no deleted user for an email whose user is enabled`() {
        // Given
        val user = userFixtures.givenAUserHistory()
            .let { User.from(it) }
        repository.saveOrUpdate(user)

        // When
        val foundUser = repository.findDeletedBy(user.email)

        // Then
        foundUser shouldBe null
    }

    @Test
    fun `finds no user by an unknown email`() {
        // Given
        val email = Email("nobody@drinkit.test")

        // When
        val foundUser = repository.findEnabledBy(email)

        // Then
        foundUser shouldBe null
    }
}
