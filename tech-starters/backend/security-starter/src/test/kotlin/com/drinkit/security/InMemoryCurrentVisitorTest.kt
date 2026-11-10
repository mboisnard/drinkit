package com.drinkit.security

internal class InMemoryCurrentVisitorTest : CurrentVisitorTestContract() {

    override fun givenAVisit(visitorId: String, ipAddress: IpAddress): CurrentVisitor =
        InMemoryCurrentVisitor(visitorId, ipAddress)
}
