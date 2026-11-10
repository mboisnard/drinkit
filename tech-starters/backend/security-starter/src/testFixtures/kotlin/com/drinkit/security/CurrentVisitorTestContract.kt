package com.drinkit.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class CurrentVisitorTestContract {

    /** The visitor of a visit that [visitorId] names, from [ipAddress]. */
    abstract fun givenAVisit(visitorId: String, ipAddress: IpAddress): CurrentVisitor

    @Test
    fun `gives the id of the visit`() {
        // Given
        val visitor = givenAVisit("6f1d2c3e-58a4-4b7e-9d0a-1c2b3d4e5f60", IpAddress("203.0.113.7"))

        // When
        val id = visitor.id()

        // Then
        id shouldBe "6f1d2c3e-58a4-4b7e-9d0a-1c2b3d4e5f60"
    }

    @Test
    fun `gives the address the visit comes from`() {
        // Given
        val visitor = givenAVisit("6f1d2c3e-58a4-4b7e-9d0a-1c2b3d4e5f60", IpAddress("2001:db8::7"))

        // When
        val ipAddress = visitor.ipAddress()

        // Then
        ipAddress shouldBe IpAddress("2001:db8::7")
    }
}
