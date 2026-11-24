package com.drinkit.featuremanagement.infra.flipt

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

private const val PROVIDER_LEVEL = "logging.level.dev.openfeature.contrib.providers.flipt.FliptProvider"

internal class FliptLoggingEnvironmentPostProcessorTest {

    @Test
    fun `silences the errors FliptProvider logs by default`() {
        // When
        val environment = withDefaults()

        // Then
        environment.getProperty(PROVIDER_LEVEL) shouldBe "off"
    }

    @Test
    fun `the application's own level wins`() {
        // When
        val environment = withDefaults(PROVIDER_LEVEL to "error")

        // Then
        environment.getProperty(PROVIDER_LEVEL) shouldBe "error"
    }

    private fun withDefaults(vararg applicationProperties: Pair<String, String>): StandardEnvironment {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("application", mapOf(*applicationProperties)))
        FliptLoggingEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())
        return environment
    }
}
