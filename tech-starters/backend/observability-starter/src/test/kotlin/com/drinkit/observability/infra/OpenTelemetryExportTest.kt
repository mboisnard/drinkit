package com.drinkit.observability.infra

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.tracing.Tracer
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration

internal class OpenTelemetryExportTest {

    @Test
    fun `exports nothing by default, even with a collector set`() {
        start().use { context ->
            context.containsBean("otlpMeterRegistry") shouldBe false
            context.getBeansOfType(SpanExporter::class.java).values.shouldBeEmpty()
        }
    }

    @Test
    fun `traces each request even when nothing is exported`() {
        start().use { context ->
            context.getBean(Tracer::class.java).shouldNotBeNull()
        }
    }

    @Test
    fun `exports metrics and traces once switched on`() {
        start("--starters.observability.opentelemetry.enabled=true").use { context ->
            context.containsBean("otlpMeterRegistry") shouldBe true
            context.getBeansOfType(SpanExporter::class.java).values.shouldNotBeEmpty()
        }
    }

    @Test
    fun `exports nothing once the starter is switched off, even with the export asked for`() {
        // When
        val switchedOff =
            start("--starters.observability.enabled=false", "--starters.observability.opentelemetry.enabled=true")

        // Then
        switchedOff.use { context ->
            context.containsBean("otlpMeterRegistry") shouldBe false
            context.getBeansOfType(SpanExporter::class.java).values.shouldBeEmpty()
        }
    }

    private fun start(vararg arguments: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(Application::class.java)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.main.banner-mode=off",
                "--management.opentelemetry.tracing.export.otlp.endpoint=http://collector.test:4318/v1/traces",
                *arguments,
            )

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class Application
}
