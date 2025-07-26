package com.drinkit.user.spi

import com.drinkit.user.UserFixtures
import com.drinkit.user.core.User
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId
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
        val savedUser = repository.findBy(user.id)

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
        val user = repository.findBy(userId)

        // Then
        user shouldBe null
    }
}