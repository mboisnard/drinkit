package com.drinkit.security.infra.login

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

private val REMEMBER_ME_REQUESTED = JsonLoginFilter::class.java.name + ".REMEMBER_ME_REQUESTED"
private const val MAX_USERNAME_LENGTH = 100
private const val MAX_PASSWORD_LENGTH = 128
private const val MAX_BODY_BYTES = 4096

/**
 * Reads the login of the single-page app from a JSON body, `{username, password, rememberMe}`, where Spring Security
 * only reads a form. It bounds the body and its fields before reading them, so that an oversized login costs no
 * password hashing, and refuses it with the same error as a wrong password. It extends the form login filter to take
 * its place in the chain, which `HttpSecurity.addFilter` finds by class.
 *
 * OWASP: ASVS 5.0 V2.2.1, V15.2.2, Input Validation Cheat Sheet (size limits before parsing).
 */
internal class JsonLoginFilter(private val jsonMapper: JsonMapper) : UsernamePasswordAuthenticationFilter() {

    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication {
        val body = readBody(request)
        request.setAttribute(REMEMBER_ME_REQUESTED, body.path("rememberMe").booleanValue(false))

        val username = body.path("username").stringValue("")
        val password = body.path("password").stringValue("")
        if (username.length > MAX_USERNAME_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
            throw BadCredentialsException("Login longer than the contract allows")
        }

        return authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(username, password),
        )
    }

    private fun readBody(request: HttpServletRequest): JsonNode {
        val bytes = request.inputStream.readNBytes(MAX_BODY_BYTES + 1)
        if (bytes.size > MAX_BODY_BYTES) throw BadCredentialsException("Login body too large")

        return try {
            jsonMapper.readTree(bytes)
        } catch (_: JacksonException) {
            throw BadCredentialsException("Unreadable login")
        }
    }

    companion object {
        fun rememberMeRequested(request: HttpServletRequest) = request.getAttribute(REMEMBER_ME_REQUESTED) == true
    }
}
