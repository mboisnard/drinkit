package com.drinkit.i18n.infra

import com.drinkit.i18n.CurrentLocale
import com.drinkit.i18n.FixedCurrentLocale
import com.drinkit.i18n.Translations
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class I18nAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(I18nAutoConfiguration::class.java))
        .withPropertyValues("starters.i18n.prefixes=cellar")

    @Test
    fun `reads the catalog and the request's language by default`() {
        runner.run { context ->
            // When & Then
            context.getBean(Translations::class.java).shouldBeInstanceOf<JsonCatalog>()
            context.getBean(CurrentLocale::class.java).shouldBeInstanceOf<RequestCurrentLocale>()
        }
    }

    @Test
    fun `backs off when the application declares its own current locale`() {
        // Given
        val withOwnLocale = runner.withBean(CurrentLocale::class.java, { FixedCurrentLocale() })

        withOwnLocale.run { context ->
            // When & Then
            context.getBean(CurrentLocale::class.java).shouldBeInstanceOf<FixedCurrentLocale>()
        }
    }

    @Test
    fun `registers nothing when disabled`() {
        // Given
        val disabled = runner.withPropertyValues("starters.i18n.enabled=false")

        disabled.run { context ->
            // When & Then
            context.containsBean("translations") shouldBe false
            context.containsBean("currentLocale") shouldBe false
        }
    }
}
