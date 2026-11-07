package com.drinkit.observability.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.tracing.propagation.Propagator
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration

internal class IncomingTraceContextTest {

    @Test
    fun `ignores the trace id a caller sends, even once the export is on`() {
        start().use { context ->
            traceIdOfRequestCarrying(context, CALLER_TRACE_PARENT) shouldNotBe CALLER_TRACE_ID
        }
    }

    @Test
    fun `follows the caller's trace id once the application trusts it`() {
        start("--management.tracing.propagation.consume=W3C").use { context ->
            traceIdOfRequestCarrying(context, CALLER_TRACE_PARENT) shouldBe CALLER_TRACE_ID
        }
    }

    private fun traceIdOfRequestCarrying(context: ConfigurableApplicationContext, traceParent: String): String {
        val headers = mapOf("traceparent" to traceParent)
        val span = context.getBean(Propagator::class.java)
            .extract(headers) { carrier, key -> carrier[key] }
            .start()
        return span.context().traceId().also { span.end() }
    }

    private fun start(vararg arguments: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(Application::class.java)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.main.banner-mode=off",
                "--starters.observability.opentelemetry.enabled=true",
                "--management.otlp.metrics.export.enabled=false",
                *arguments,
            )

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class Application

    private companion object {
        const val CALLER_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"
        const val CALLER_TRACE_PARENT = "00-$CALLER_TRACE_ID-00f067aa0ba902b7-01"
    }
}
