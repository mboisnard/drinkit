package com.drinkit.security

class InMemoryCurrentVisitor(
    private val visitorId: VisitorId = VisitorId.create(),
    private val address: IpAddress = IpAddress("203.0.113.7"),
) : CurrentVisitor {

    override fun id() = visitorId

    override fun ipAddress() = address
}
