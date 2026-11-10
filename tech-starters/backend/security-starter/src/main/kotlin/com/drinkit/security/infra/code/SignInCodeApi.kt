package com.drinkit.security.infra.code

import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.rate.limit.RateLimitDecision.Allowed
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.infra.SecurityErrorResponses
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

internal class SignInCodeApi(
    private val issuer: SignInCodeIssuer,
    private val limits: SignInCodeLimits,
    private val visitor: CurrentVisitor,
    private val errorResponses: SecurityErrorResponses,
    private val jsonMapper: JsonMapper,
    private val maxEmailLength: Int,
) {

    // The IP address counts first, and a limited email keeps its live code, which a new one would replace
    fun requestCode(request: ServerRequest): ServerResponse {
        val servletRequest = request.servletRequest()

        val fromIp = limits.requestFrom(visitor.ipAddress())
        val login = servletRequest.readSignInBody(jsonMapper)?.login(maxEmailLength)
        val forEmail = if (fromIp is Allowed && login != null) limits.requestFor(login) else null

        return when {
            fromIp is RateLimitDecision.Refused -> errorResponses.tooManyRequests(servletRequest, fromIp)
            login == null -> ServerResponse.badRequest().build()
            forEmail is RateLimitDecision.Refused -> errorResponses.tooManyRequests(servletRequest, forEmail)
            else -> ServerResponse.noContent().build().also { issuer.issue(login, servletRequest.locale) }
        }
    }
}
