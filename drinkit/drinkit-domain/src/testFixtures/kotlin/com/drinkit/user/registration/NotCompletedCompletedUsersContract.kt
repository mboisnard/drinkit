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

    @Test
    fun `should find not completed user by id`() {
        val user = UserFixtures.givenANotCompletedUser()
        repository.create(user)

        val notCompletedUser = repository.findById(user.id)

        notCompletedUser?.id shouldBe user.id
        notCompletedUser?.email shouldBe user.email
        notCompletedUser?.completed shouldBe false
        notCompletedUser?.enabled shouldBe true
    }

    @Test
    fun `should not fail when trying to create an already existing not completed user`() {
        val user = UserFixtures.givenANotCompletedUser()
        val savedUserId = repository.create(user)

        savedUserId shouldBe user.id

        val nullWhenUserAlreadyExists = repository.create(user)

        nullWhenUserAlreadyExists shouldBe null
    }

    @Test
    fun `should update an existing not completed user`() {
        val user = UserFixtures.givenANotCompletedUser()
        val savedUserId = repository.create(user)

        savedUserId shouldBe user.id

        val notCompletedUser = repository.findById(user.id)

        notCompletedUser?.id shouldBe user.id
        notCompletedUser?.firstname shouldBe null
        notCompletedUser?.lastName shouldBe null
        notCompletedUser?.email shouldBe user.email
        notCompletedUser?.completed shouldBe false
        notCompletedUser?.enabled shouldBe true

        repository.update(user)

        val updatedNotCompletedUser = repository.findById(user.id)

        updatedNotCompletedUser?.id shouldBe user.id
        updatedNotCompletedUser?.firstname shouldBe user.firstname
        updatedNotCompletedUser?.lastName shouldBe user.lastName
    }
}