package com.drinkit.i18n.infra

import com.drinkit.i18n.CurrentLocale
import com.drinkit.i18n.CurrentLocaleTestContract
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Locale

internal class RequestCurrentLocaleTest : CurrentLocaleTestContract() {

    private val currentLocale = RequestCurrentLocale(JsonCatalog("classpath*:translations/*.json", setOf("cellar")))

    override fun givenARequestIn(language: Locale): CurrentLocale {
        givenARequest(acceptLanguage = "${language.language}-FR")

        return currentLocale
    }

    @Test
    fun `a request reads in the first of its languages the catalog holds`() {
        // Given
        givenARequest(acceptLanguage = "zh-CN, fr;q=0.8, en;q=0.5")

        // When
        val locale = currentLocale.get()

        // Then
        locale shouldBe Locale.FRENCH
    }

    @Test
    fun `a request in languages the catalog lacks reads in English`() {
        // Given
        givenARequest(acceptLanguage = "zh-CN, it;q=0.8")

        // When
        val locale = currentLocale.get()

        // Then
        locale shouldBe Locale.ENGLISH
    }

    @Test
    fun `a request that names no language reads in English`() {
        // Given
        givenARequest(acceptLanguage = null)

        // When
        val locale = currentLocale.get()

        // Then
        locale shouldBe Locale.ENGLISH
    }

    @Test
    fun `a language kept for the session that the catalog lacks gives way to the request's`() {
        // Given
        givenARequest(acceptLanguage = "fr-FR")
        currentLocale.keepForTheSession(Locale.CHINESE)

        // When
        val locale = currentLocale.get()

        // Then
        locale shouldBe Locale.FRENCH
    }

    @AfterEach
    fun leaveTheRequest() = RequestContextHolder.resetRequestAttributes()

    private fun givenARequest(acceptLanguage: String?) {
        val request = MockHttpServletRequest()
        acceptLanguage?.let { request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, it) }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }
}
