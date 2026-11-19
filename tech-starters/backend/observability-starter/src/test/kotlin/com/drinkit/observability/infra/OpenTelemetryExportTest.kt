package com.drinkit.observability.infra

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.tracing.Tracer
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit.SECONDS

internal class OpenTelemetryExportTest {

    @Test
    fun `exports nothing by default, even with a collector set`() {
        // When
        val byDefault = start()

        // Then
        byDefault.use { context ->
            context.containsBean("otlpMeterRegistry") shouldBe false
            context.getBeansOfType(SpanExporter::class.java).values.shouldBeEmpty()
            context.getBeansOfType(OpenTelemetryLogs::class.java).values.shouldBeEmpty()
        }
    }

    @Test
    fun `traces each request even when nothing is exported`() {
        // When
        val byDefault = start()

        // Then
        byDefault.use { context ->
            context.getBean(Tracer::class.java).shouldNotBeNull()
        }
    }

    @Test
    fun `exports metrics and traces once switched on`() {
        // When
        val switchedOn = start("--starters.observability.opentelemetry.enabled=true")

        // Then
        switchedOn.use { context ->
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

    @Test
    fun `exports each log with the trace it was written in once switched on`() {
        // Given
        val switchedOn = start("--starters.observability.opentelemetry.enabled=true")

        switchedOn.use { context ->
            val tracer = context.getBean(Tracer::class.java)
            val span = tracer.nextSpan().name("request").start()

            // When
            tracer.withSpan(span).use { LoggerFactory.getLogger("test").info("A log within a request") }
            span.end()

            // Then
            context.getBean(SdkLoggerProvider::class.java).forceFlush().join(5, SECONDS)
            val log = context.getBean(InMemoryLogRecordExporter::class.java).finishedLogRecordItems
                .single { it.bodyValue?.asString() == "A log within a request" }
            log.spanContext.traceId shouldBe span.context().traceId()
        }
    }

    @Test
    fun `exports the logs written before the SDK was ready`() {
        // When
        val switchedOn = start("--starters.observability.opentelemetry.enabled=true")

        // Then
        switchedOn.use { context ->
            context.getBean(SdkLoggerProvider::class.java).forceFlush().join(5, SECONDS)
            val logs = context.getBean(InMemoryLogRecordExporter::class.java).finishedLogRecordItems
            logs.any { it.bodyValue?.asString().orEmpty().startsWith("Starting ") } shouldBe true
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
    class Application {

        @Bean
        fun inMemoryLogRecordExporter(): InMemoryLogRecordExporter = InMemoryLogRecordExporter.create()
    }
}
