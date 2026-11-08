package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl
import org.springframework.security.web.util.matcher.RequestMatcher

/** Applied in `@Order`, before the starter refuses any request no rule matches. */
@TechStarterTool
interface EndpointRules {

    fun declare(rules: AuthorizeHttpRequestsDsl)

    /** Requests that change no state, freed from the CSRF token. Fetch Metadata still refuses them cross-site. */
    fun csrfFree(): List<RequestMatcher> = emptyList()
}

@TechStarterTool
fun endpointRules(
    csrfFree: List<RequestMatcher> = emptyList(),
    block: AuthorizeHttpRequestsDsl.() -> Unit,
): EndpointRules = object : EndpointRules {
    override fun declare(rules: AuthorizeHttpRequestsDsl) = rules.block()

    override fun csrfFree() = csrfFree
}
