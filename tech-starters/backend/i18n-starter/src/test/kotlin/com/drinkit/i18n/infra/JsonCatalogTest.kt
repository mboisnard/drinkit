package com.drinkit.i18n.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Locale

internal class JsonCatalogTest {

    private val catalog = JsonCatalog("classpath*:translations/*.json", prefixes = setOf("cellar"))

    @Test
    fun `a message reads in the language of the locale, whatever its region`() {
        // When
        val message = catalog.translate("cellar.welcome", Locale.CANADA_FRENCH, mapOf("cellarName" to "La Cave"))

        // Then
        message shouldBe "Bienvenue dans La Cave"
    }

    @Test
    fun `a language the catalog lacks reads in English`() {
        // When
        val message = catalog.translate("cellar.welcome", Locale.ITALIAN, mapOf("cellarName" to "La Cave"))

        // Then
        message shouldBe "Welcome to La Cave"
    }

    // The messages and the values of the frontend i18n-starter's test, so that ICU4J reads them as formatjs does
    @ParameterizedTest
    @CsvSource(
        "en, 0, 0 bottles",
        "en, 1, 1 bottle",
        "en, 2, 2 bottles",
        "fr, 0, aucune bouteille",
        "fr, 1, 1 bouteille",
        "fr, 2, 2 bouteilles",
    )
    fun `a plural follows the rules of each language as the frontend formats it`(
        language: String,
        count: Int,
        expected: String,
    ) {
        // When
        val message = catalog.translate("cellar.bottles", Locale.forLanguageTag(language), mapOf("count" to count))

        // Then
        message shouldBe expected
    }

    @Test
    fun `a key of a prefix the application does not declare is not loaded`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { catalog.translate("hidden.secret", Locale.ENGLISH) }
    }

    @Test
    fun `a key the catalog lacks fails`() {
        // When & Then
        shouldThrow<IllegalArgumentException> { catalog.translate("cellar.unknown", Locale.ENGLISH) }
    }
}
