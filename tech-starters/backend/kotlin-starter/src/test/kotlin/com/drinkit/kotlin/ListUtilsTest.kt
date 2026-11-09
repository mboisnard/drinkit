package com.drinkit.kotlin

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

internal class ListUtilsTest {

    @Test
    fun `a value is added when its condition is false`() {
        // When
        val errors = mutableListOf<String>().apply { addUnless(false, "Cellar name is too long") }

        // Then
        errors shouldContainExactly listOf("Cellar name is too long")
    }

    @Test
    fun `a value is left out when its condition is true`() {
        // When
        val errors = mutableListOf<String>().apply { addUnless(true, "Cellar name is too long") }

        // Then
        errors.shouldBeEmpty()
    }
}
