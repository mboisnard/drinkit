package com.drinkit.security.infra.session

import com.drinkit.security.sessionKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.module.kotlin.jacksonMapperBuilder

internal class HttpSessionDataTest {

    private data class Preferences(val currency: String, val favorites: List<String>)

    private val preferences = sessionKey<Preferences>("preferences")

    private val request = MockHttpServletRequest().also {
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(it))
    }
    private val sessionData = HttpSessionData(jacksonMapperBuilder().build())

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
    fun `opens the session to keep a value`() {
        // When
        sessionData[preferences] = Preferences("EUR", listOf("Margaux"))

        // Then
        request.getSession(false).shouldNotBeNull()
        sessionData[preferences] shouldBe Preferences("EUR", listOf("Margaux"))
    }
}
