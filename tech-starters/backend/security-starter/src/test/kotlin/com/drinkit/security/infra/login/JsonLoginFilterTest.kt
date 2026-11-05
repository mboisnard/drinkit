package com.drinkit.security.infra.login

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import tools.jackson.databind.json.JsonMapper

internal class JsonLoginFilterTest {

    private val filter = JsonLoginFilter(JsonMapper.shared()).apply {
        setAuthenticationManager(AuthenticationManager { error("Hashed the password of an oversized login") })
    }

    @Test
    fun `refuses a username over 100 characters as a wrong password, before hashing anything`() {
        shouldThrow<BadCredentialsException> { login("""{"username": "${"a".repeat(101)}", "password": "p"}""") }
    }

    @Test
    fun `refuses a password over 128 characters as a wrong password, before hashing it`() {
        shouldThrow<BadCredentialsException> { login("""{"username": "ada", "password": "${"a".repeat(129)}"}""") }
    }

    @Test
    fun `refuses a login body over 4 KiB before reading it`() {
        shouldThrow<BadCredentialsException> {
            login("""{"username": "ada", "password": "p", "padding": "${"a".repeat(5_000)}"}""")
        }
    }

    private fun login(body: String) = filter.attemptAuthentication(
        MockHttpServletRequest("POST", LOGIN_PATH).apply { setContent(body.toByteArray()) },
        MockHttpServletResponse(),
    )
}
