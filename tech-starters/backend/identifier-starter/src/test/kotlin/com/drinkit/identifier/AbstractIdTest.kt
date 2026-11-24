package com.drinkit.identifier

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class OrderId(value: String) : AbstractId(value) {
    companion object : IdFactory<OrderId>(::OrderId)
}

private class CustomerId(value: String) : AbstractId(value)

internal data class Order(val id: OrderId)

@OptIn(ExperimentalUuidApi::class)
internal class AbstractIdTest {

    private val mapper = JsonMapper.builder().addModule(kotlinModule()).build()

    @Test
    fun `a new identifier is 26 lowercase Crockford base32 characters`() {
        // When
        val id = OrderId.create().value

        // Then
        id shouldMatch "[0-7][0-9a-hjkmnp-tv-z]{25}"
    }

    @Test
    fun `an identifier is its UUID written in Crockford base32`() {
        // Given
        val uuid = Uuid.parse("01a20d1a-f440-71a2-bc4d-5e6f708192a3")

        // When
        val id = uuid.toCrockford()

        // Then
        id shouldBe "01m86hnx20e6hbrkaydxr834n3"
    }

    @Test
    fun `an identifier drawn later sorts after an earlier one`() {
        // When
        val ids = List(1_000) { OrderId.create().value }

        // Then
        ids.shouldBeStrictlyIncreasing()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "659ee3164b1d53340c4f7608",
            "01M86HNX20E6HBRKAYDXR834N3",
            "01m86hnx20e6hbrkaydxr834nu",
            "01m86hnx20e6hbrkaydxr834n",
            "01m86hnx20e6hbrkaydxr834n30",
            "81m86hnx20e6hbrkaydxr834n3",
        ],
    )
    fun `an identifier refuses a value of another format`(value: String) {
        // When & Then
        shouldThrow<IllegalArgumentException> { OrderId(value) }
    }

    @Test
    fun `identifiers of two kinds are different even with the same value`() {
        // Given
        val id = OrderId.create()

        // When & Then
        CustomerId(id.value) shouldNotBe id
    }

    @Test
    fun `a value of the format parses into its identifier`() {
        // Given
        val id = OrderId.create()

        // When
        val parsed = OrderId.parseOrNull(id.value)

        // Then
        parsed shouldBe id
    }

    @Test
    fun `a value of another format parses into nothing`() {
        // When
        val id = OrderId.parseOrNull("not-an-id")

        // Then
        id.shouldBeNull()
    }

    @Test
    fun `an identifier is written in JSON as its value`() {
        // Given
        val id = OrderId.create()

        // When
        val json = mapper.writeValueAsString(Order(id))

        // Then
        json shouldBe """{"id":"${id.value}"}"""
    }

    @Test
    fun `an identifier is read from JSON as its value`() {
        // Given
        val id = OrderId.create()

        // When
        val order = mapper.readValue<Order>("""{"id":"${id.value}"}""")

        // Then
        order shouldBe Order(id)
    }

    @Test
    fun `identifiers of one kind with the same value are equal`() {
        // Given
        val id = OrderId.create()

        // When & Then
        OrderId(id.value) shouldBe id
    }
}
