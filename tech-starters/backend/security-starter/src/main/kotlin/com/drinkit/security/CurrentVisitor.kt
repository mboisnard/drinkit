package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

/** The browser behind the request, signed in or not, which a cookie keeps from one visit to the next. */
@TechStarterTool
interface CurrentVisitor {
    fun id(): String

    /** The client's address, read through the proxies that `server.forward-headers-strategy` trusts. */
    fun ipAddress(): IpAddress
}
