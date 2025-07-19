package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.CorrelationId
import com.drinkit.user.CreateNewUser.Result.UserAlreadyExists
import com.drinkit.user.CreateNewUser.Result.UserCreated
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.Password
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class CreateNewUserTest {

    private val userFixtures = UserFixtures()
    private val createNewUser = userFixtures.createNewUser

    @Test
    fun `should create a new user`() {
        // Given
        val command = CreateNewUserCommand(
            email = Email("john.doe@gmail.com"),
            password = EncodedPassword.from(Password("F@kePa$\$w0rD")) { it },
            author = Author.Unlogged(CorrelationId.create())
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
            email = Email("john.doe@gmail.com"),
            password = EncodedPassword.from(Password("F@kePa$\$w0rD")) { it },
            author = Author.Unlogged(CorrelationId.create())
        )
        createNewUser.invoke(command)

        // When
        val response = createNewUser.invoke(command)

        // Then
        response.shouldBeInstanceOf<UserAlreadyExists>()
    }
}