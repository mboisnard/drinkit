package com.drinkit.observability.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

internal class ObservabilityDefaultsEnvironmentPostProcessorTest {

    @Test
    fun `no endpoint can change the application`() {
        // When
        val environment = withDefaults()

        // Then
        environment.getProperty("management.endpoints.access.max-permitted") shouldBe "read-only"
    }

    @Test
    fun `the application's own exposure wins`() {
        // When
        val environment = withDefaults("management.endpoints.web.exposure.include" to "health")

        // Then
        environment.getProperty("management.endpoints.web.exposure.include") shouldBe "health"
    }

    @Test
    fun `adds no default when the starter is disabled`() {
        // When
        val environment = withDefaults("starters.observability.enabled" to "false")

        // Then
        environment.getProperty("management.endpoints.web.exposure.include") shouldBe null
    }

    private fun withDefaults(vararg applicationProperties: Pair<String, String>): StandardEnvironment {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("application", mapOf(*applicationProperties)))
        ObservabilityDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())

        return environment
    }
}
