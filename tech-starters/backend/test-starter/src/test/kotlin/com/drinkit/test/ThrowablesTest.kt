package com.drinkit.test

import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

internal class ThrowablesTest {

    @Test
    fun `the root cause of an exception is the deepest of its causes`() {
        // Given
        val root = IllegalStateException("root")
        val wrapped = RuntimeException("outer", IllegalArgumentException("middle", root))

        // When
        val rootCause = wrapped.rootCause()

        // Then
        rootCause shouldBeSameInstanceAs root
    }
}
