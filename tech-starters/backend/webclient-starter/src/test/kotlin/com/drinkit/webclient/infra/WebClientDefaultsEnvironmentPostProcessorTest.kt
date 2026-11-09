package com.drinkit.webclient.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

internal class WebClientDefaultsEnvironmentPostProcessorTest {

    @Test
    fun `every HTTP client gets default timeouts`() {
        // When
        val environment = withDefaults()

        // Then
        environment.getProperty("spring.http.clients.connect-timeout") shouldBe "2s"
        environment.getProperty("spring.http.clients.read-timeout") shouldBe "5s"
    }

    @Test
    fun `the application's own timeouts win`() {
        // When
        val environment = withDefaults("spring.http.clients.read-timeout" to "30s")

        // Then
        environment.getProperty("spring.http.clients.read-timeout") shouldBe "30s"
    }

    @Test
    fun `adds no default timeout when the starter is disabled`() {
        // When
        val environment = withDefaults("starters.webclient.enabled" to "false")

        // Then
        environment.getProperty("spring.http.clients.connect-timeout") shouldBe null
    }

    private fun withDefaults(vararg applicationProperties: Pair<String, String>): StandardEnvironment {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("application", mapOf(*applicationProperties)))
        WebClientDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())
        return environment
    }
}
