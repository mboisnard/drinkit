package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.CorrelationId
import com.drinkit.user.CreateNewUser.Result.UserAlreadyExists
import com.drinkit.user.CreateNewUser.Result.UserCreated
import com.drinkit.user.UserFixtures.Companion.VALID_EMAIL
import com.drinkit.user.UserFixtures.Companion.VALID_PASSWORD
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.Locale

internal class CreateNewUserTest {

    private val userFixtures = UserFixtures()
    private val createNewUser = userFixtures.createNewUser

    @Test
    fun `should create a new user`() {
        // Given
        val command = CreateNewUserCommand(
            email = VALID_EMAIL,
            password = VALID_PASSWORD,
            author = Author.Unlogged(CorrelationId.create()),
            locale = Locale.FRANCE,
        )

        // When
        val response = createNewUser.invoke(command)

        // Then
        response.shouldBeInstanceOf<UserCreated>()
    }

    @Test
    fun `should check if a user already exists with a given email`() {
        // Given
        val command = CreateNewUserCommand(
            email = VALID_EMAIL,
            password = VALID_PASSWORD,
            author = Author.Unlogged(CorrelationId.create()),
            locale = Locale.FRANCE,
        )
        createNewUser.invoke(command)

        // When
        val response = createNewUser.invoke(command)

        // Then
        response.shouldBeInstanceOf<UserAlreadyExists>()
    }
}