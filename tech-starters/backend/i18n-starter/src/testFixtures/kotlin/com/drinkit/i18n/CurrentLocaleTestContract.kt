package com.drinkit.i18n

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

abstract class CurrentLocaleTestContract {

    /** The current locale of a request that reads in [language]. */
    abstract fun givenARequestIn(language: Locale): CurrentLocale

    @Test
    fun `gives the language the request reads in`() {
        // Given
        val currentLocale = givenARequestIn(Locale.FRENCH)

        // When
        val locale = currentLocale.get()

        // Then
        locale shouldBe Locale.FRENCH
    }

    @Test
    fun `a language kept for the session wins over the request's`() {
        // Given
        val currentLocale = givenARequestIn(Locale.ENGLISH)
        currentLocale.keepForTheSession(Locale.FRENCH)

        // When
        val locale = currentLocale.get()

        // Then
        locale shouldBe Locale.FRENCH
    }
}
