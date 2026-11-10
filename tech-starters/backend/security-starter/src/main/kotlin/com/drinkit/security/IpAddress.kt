package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.net.Inet6Address
import java.net.InetAddress

private const val IPV6_NETWORK_BYTES = 8
private const val IPV6_BYTES = 16

/** The client's address as the request gives it: a proxy may forward what is no IP address, which stays as it is. */
@TechStarterTool
data class IpAddress(val value: String) {

    /**
     * What one host holds, to count it once: an IPv4 address, or the whole IPv6 network of 64 bits that a host often
     * gets, which would otherwise give it countless addresses.
     */
    fun host(): String = when (val address = runCatching { InetAddress.ofLiteral(value) }.getOrNull()) {
        is Inet6Address -> {
            val network = address.address.copyOf(IPV6_NETWORK_BYTES).copyOf(IPV6_BYTES)
            "${InetAddress.getByAddress(network).hostAddress}/64"
        }

        null -> value

        else -> address.hostAddress
    }
}
