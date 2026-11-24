package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.drinkit.identifier.AbstractId
import com.drinkit.identifier.IdFactory

/** The browser behind the request, signed in or not, which a cookie keeps from one visit to the next. */
@TechStarterTool
interface CurrentVisitor {
    fun id(): VisitorId

    /** The client's address, read through the proxies that `server.forward-headers-strategy` trusts. */
    fun ipAddress(): IpAddress
}

@TechStarterTool
class VisitorId(value: String) : AbstractId(value) {
    companion object : IdFactory<VisitorId>(::VisitorId)
}
