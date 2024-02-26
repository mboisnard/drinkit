package com.drinkit.user.registration

import com.drinkit.user.UserFixtures
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class NotCompletedCompletedUsersContract {

    private val repository: NotCompletedUsers by lazy {
        fetchRepository()
    }

    abstract fun fetchRepository(): NotCompletedUsers

    @Test
    fun `should find not completed user by his email`() {
        val user = UserFixtures.givenANotCompletedUser()
        repository.create(user)

        val isUserExistsInDatabase = repository.emailExists(user.email)

        isUserExistsInDatabase shouldBe true
    }
}