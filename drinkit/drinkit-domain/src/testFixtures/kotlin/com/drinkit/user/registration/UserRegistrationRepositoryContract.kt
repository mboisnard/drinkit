package com.drinkit.user.registration

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class UserRegistrationRepositoryContract {

    private val repository: UserRegistrationRepository by lazy {
        fetchRepository()
    }

    abstract fun fetchRepository(): UserRegistrationRepository

    @Test
    fun `should find user by his email`() {
        val user = UserUtils.givenANotCompletedUser()
        repository.create(user)

        val isUserExistsInDatabase = repository.emailExists(user.email)

        isUserExistsInDatabase shouldBe true
    }
}