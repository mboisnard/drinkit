package com.drinkit.observability.infra

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration
import org.springframework.boot.opentelemetry.autoconfigure.logging.ConditionalOnEnabledLoggingExport
import org.springframework.context.annotation.Bean

/** Sends each log event over OTLP too, with the trace and the span it was written in. */
@AutoConfiguration(after = [OpenTelemetrySdkAutoConfiguration::class])
@ConditionalOnEnabledLoggingExport("otlp")
@ConditionalOnBean(OpenTelemetry::class)
internal class OpenTelemetryLogsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun openTelemetryLogs(openTelemetry: OpenTelemetry) = OpenTelemetryLogs(openTelemetry)
}

/** Hands the SDK to the appender of [OpenTelemetryLogsListener], which sends the logs it held meanwhile. */
internal class OpenTelemetryLogs(private val openTelemetry: OpenTelemetry) : InitializingBean {

    override fun afterPropertiesSet() = OpenTelemetryAppender.install(openTelemetry)
}
