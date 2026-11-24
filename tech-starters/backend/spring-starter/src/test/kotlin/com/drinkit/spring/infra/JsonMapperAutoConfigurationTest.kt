package com.drinkit.spring.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

internal data class Bottle(val name: String = "unnamed")

internal class JsonMapperAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(JacksonAutoConfiguration::class.java, JsonMapperAutoConfiguration::class.java),
        )

    @Test
    fun `reads a null as the default value of the property`() {
        runner.run { context ->
            // When
            val bottle = context.getBean(JsonMapper::class.java).readValue<Bottle>("""{"name":null}""")

            // Then
            bottle shouldBe Bottle()
        }
    }

    @Test
    fun `leaves the mapper to Spring Boot when disabled`() {
        // Given
        val disabled = runner.withPropertyValues("starters.spring.enabled=false")

        disabled.run { context ->
            // When & Then
            context.containsBean("applicationJsonMapperBuilderCustomizer") shouldBe false
        }
    }
}
