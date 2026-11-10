package com.drinkit.security

import java.util.UUID

class InMemoryCurrentVisitor(
    private val visitorId: String = UUID.randomUUID().toString(),
    private val address: IpAddress = IpAddress("203.0.113.7"),
) : CurrentVisitor {

    override fun id() = visitorId

    override fun ipAddress() = address
}
