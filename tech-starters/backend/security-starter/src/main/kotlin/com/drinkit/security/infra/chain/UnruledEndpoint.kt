package com.drinkit.security.infra.chain

import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import java.util.function.Supplier

/**
 * Its own [Refused] decision lets `EndpointRulesCoverage` list the endpoints no rule declares.
 *
 * OWASP: ASVS 5.0 V8.2.1, Authorization Cheat Sheet (deny by default).
 */
internal object UnruledEndpoint : AuthorizationManager<RequestAuthorizationContext> {

    // A data object, which Kotlin gives the readResolve a serializable object needs to stay a singleton
    data object Refused : AuthorizationDecision(false)

    override fun authorize(authentication: Supplier<out Authentication?>, context: RequestAuthorizationContext) =
        Refused
}
