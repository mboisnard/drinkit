package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl

/** Applied in `@Order`, before the starter refuses any request no rule matches. */
@TechStarterTool
fun interface EndpointRules {
    fun declare(rules: AuthorizeHttpRequestsDsl)
}

@TechStarterTool
fun endpointRules(block: AuthorizeHttpRequestsDsl.() -> Unit): EndpointRules = EndpointRules { it.block() }
