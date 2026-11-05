package com.drinkit.user.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

internal class PasswordTest {

    @Test
    fun `refuses a password over 128 characters, which the login would refuse`() {
        // Given
        val tooLong = "Aa1!" + "a".repeat(125)

        // When & Then
        shouldThrow<IllegalArgumentException> { Password(tooLong) }
    }

    @Test
    fun `accepts a password of 128 characters`() {
        // Given
        val longest = "Aa1!" + "a".repeat(124)

        // When & Then
        shouldNotThrowAny { Password(longest) }
    }
}
