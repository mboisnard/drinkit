package com.drinkit.common

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test

internal class GenerateObjectIdTest {

    private val generateObjectId = GenerateObjectId()

    internal class TestId(override val value: String): AbstractId(value)

    @Test
    fun `should generate a valid ObjectId for the given Id class`() {
        // When
        val id = generateObjectId.invoke(TestId::class)

        // Then
        id::class shouldBe TestId::class
        id.value.length shouldBe 24
        id.value shouldMatch "[0-9a-f]{24}".toRegex()
    }

    @Test
    fun `should generate different IDs for multiple calls`() {
        // When
        val id1 = generateObjectId.invoke(TestId::class)
        val id2 = generateObjectId.invoke(TestId::class)

        // Then
        id1 shouldNotBe id2
        id1.value shouldNotBe id2.value
    }
}
