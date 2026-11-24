package com.drinkit.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

abstract class CurrentVisitorTestContract {

    /** The visitor of a visit that [visitorId] names, from [ipAddress]. */
    abstract fun givenAVisit(visitorId: VisitorId, ipAddress: IpAddress): CurrentVisitor

    @Test
    fun `gives the id of the visit`() {
        // Given
        val visitorId = VisitorId.create()
        val visitor = givenAVisit(visitorId, IpAddress("203.0.113.7"))

        // When
        val id = visitor.id()

        // Then
        id shouldBe visitorId
    }

    @Test
    fun `gives the address the visit comes from`() {
        // Given
        val visitor = givenAVisit(VisitorId.create(), IpAddress("2001:db8::7"))

        // When
        val ipAddress = visitor.ipAddress()

        // Then
        ipAddress shouldBe IpAddress("2001:db8::7")
    }
}
