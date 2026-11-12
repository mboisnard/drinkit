package com.drinkit.security.infra.code

import com.drinkit.security.CurrentVisitor
import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

internal class SignInCodeApi(
    private val issuer: SignInCodeIssuer,
    private val limits: SignInCodeLimits,
    private val visitor: CurrentVisitor,
    private val jsonMapper: JsonMapper,
    private val maxEmailLength: Int,
) {

    // The IP address counts first, and a limited email keeps its live code, which a new one would replace
    fun requestCode(request: ServerRequest): ServerResponse {
        val servletRequest = request.servletRequest()

        limits.requestFrom(visitor.ipAddress()).orThrow()
        val login = servletRequest.readSignInBody(jsonMapper)?.login(maxEmailLength)
            ?: throw ErrorResponseException(HttpStatus.BAD_REQUEST)
        limits.requestFor(login).orThrow()

        issuer.issue(login, servletRequest.locale)

        return ServerResponse.noContent().build()
    }
}
