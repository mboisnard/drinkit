package com.drinkit.security.infra.code

import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

internal class SignInCodeApi(
    private val issuer: SignInCodeIssuer,
    private val jsonMapper: JsonMapper,
    private val maxEmailLength: Int,
) {

    fun requestCode(request: ServerRequest): ServerResponse {
        val servletRequest = request.servletRequest()

        return when (val login = servletRequest.readSignInBody(jsonMapper)?.login(maxEmailLength)) {
            null -> ServerResponse.badRequest().build()
            else -> ServerResponse.noContent().build().also { issuer.issue(login, servletRequest.locale) }
        }
    }
}
