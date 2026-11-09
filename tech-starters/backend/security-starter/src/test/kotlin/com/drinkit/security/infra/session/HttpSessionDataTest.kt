package com.drinkit.security.infra.session

import com.drinkit.security.InMemoryCurrentUser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.UserAccount
import com.drinkit.security.sessionKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.module.kotlin.jacksonMapperBuilder

internal class HttpSessionDataTest {

    private data class Preferences(val currency: String, val favorites: List<String>)

    private val preferences = sessionKey<Preferences>("preferences")

    private val request = MockHttpServletRequest().also {
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(it))
    }
    private val accounts = InMemoryUserAccounts("ada@drinkit.test" to UserAccount("user-1", setOf("ROLE_USER")))
    private val currentUser = InMemoryCurrentUser(accounts).also { it.signIn("user-1") }
    private val sessionData = HttpSessionData(currentUser, jacksonMapperBuilder().build())

    @AfterEach
    fun forgetRequest() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `reads nothing under a key never written`() {
        // When
        val read = sessionData[preferences]

        // Then
        read.shouldBeNull()
    }

    @Test
    fun `reads as absent a value stored in another shape`() {
        // Given
        sessionData[sessionKey<String>("preferences")] = "EUR"

        // When
        val read = sessionData[preferences]

        // Then
        read.shouldBeNull()
    }

    @Test
    fun `forgets a removed value`() {
        // Given
        sessionData[preferences] = Preferences("EUR", listOf("Margaux"))

        // When
        sessionData.remove(preferences)

        // Then
        sessionData[preferences].shouldBeNull()
    }

    @Test
    fun `opens no session to read a value`() {
        // When
        sessionData[preferences]

        // Then
        request.getSession(false).shouldBeNull()
    }

    @Test
    fun `refuses to keep a value for an anonymous caller, and opens no session`() {
        // Given
        val anonymousSessionData = HttpSessionData(InMemoryCurrentUser(), jacksonMapperBuilder().build())

        // When & Then
        shouldThrow<AuthenticationCredentialsNotFoundException> {
            anonymousSessionData[preferences] = Preferences("EUR", emptyList())
        }
        request.getSession(false).shouldBeNull()
    }
}
