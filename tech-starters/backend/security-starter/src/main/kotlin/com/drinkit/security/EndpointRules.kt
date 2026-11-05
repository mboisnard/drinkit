package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl

/**
 * The access rules of the application's endpoints, applied in `@Order` before the starter refuses any request no
 * rule matches. Built by [endpointRules].
 */
@TechStarterTool
fun interface EndpointRules {
    fun declare(rules: AuthorizeHttpRequestsDsl)
}

/** Declares access rules with the Kotlin DSL of Spring Security: `authorize("/api/cellars", hasRole("USER"))`. */
@TechStarterTool
fun endpointRules(block: AuthorizeHttpRequestsDsl.() -> Unit): EndpointRules = EndpointRules { it.block() }
