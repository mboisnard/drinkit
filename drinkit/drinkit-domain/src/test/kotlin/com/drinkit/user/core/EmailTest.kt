package com.drinkit.user.core

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

internal class EmailTest {

    @Test
    fun `the errors of an email name the broken rule, never the submitted address`() {
        // Given
        val email = Email("jane.doe" + "e".repeat(100) + "@")

        // When
        val errors = email.validate()

        // Then
        errors shouldHaveSize 2
        errors.joinToString() shouldNotContain "jane.doe"
    }
}
