package com.drinkit.postgresql.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

internal class PostgresqlDefaultsEnvironmentPostProcessorTest {

    @Test
    fun `names neither the pool nor the sessions when the application has no name`() {
        // When
        val environment = withDefaults()

        // Then
        environment.getProperty("spring.datasource.hikari.pool-name") shouldBe null
        environment.getProperty("spring.datasource.hikari.data-source-properties.ApplicationName") shouldBe null
    }

    @Test
    fun `adds no default when the starter is disabled`() {
        // When
        val environment = withDefaults("starters.postgresql.enabled" to "false")

        // Then
        environment.getProperty("spring.datasource.hikari.auto-commit") shouldBe null
    }

    private fun withDefaults(vararg applicationProperties: Pair<String, String>): StandardEnvironment {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("application", mapOf(*applicationProperties)))
        PostgresqlDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())
        return environment
    }
}
