package com.drinkit.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class IpAddressTest {

    @Test
    fun `gives the addresses of one IPv6 network the same host`() {
        // When
        val hosts = listOf("2001:db8:5:7::1", "2001:db8:5:7:ffff::1").map { IpAddress(it).host() }

        // Then
        hosts shouldBe listOf("2001:db8:5:7:0:0:0:0/64", "2001:db8:5:7:0:0:0:0/64")
    }

    @Test
    fun `gives an IPv4 address a host of its own`() {
        // When
        val host = IpAddress("203.0.113.7").host()

        // Then
        host shouldBe "203.0.113.7"
    }

    @Test
    fun `keeps as it is what a proxy forwards that is no IP address`() {
        // When
        val host = IpAddress("unknown").host()

        // Then
        host shouldBe "unknown"
    }
}
