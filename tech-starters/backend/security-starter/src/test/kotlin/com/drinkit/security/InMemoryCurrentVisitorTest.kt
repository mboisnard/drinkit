package com.drinkit.security

internal class InMemoryCurrentVisitorTest : CurrentVisitorTestContract() {

    override fun givenAVisit(visitorId: VisitorId, ipAddress: IpAddress): CurrentVisitor =
        InMemoryCurrentVisitor(visitorId, ipAddress)
}
