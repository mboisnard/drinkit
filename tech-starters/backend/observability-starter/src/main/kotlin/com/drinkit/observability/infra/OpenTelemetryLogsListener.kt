package com.drinkit.observability.infra

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.boot.context.logging.LoggingApplicationListener
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.SmartApplicationListener
import org.springframework.core.env.getProperty

/**
 * Adds an appender to the root logger, rather than to a `logback-spring.xml` each application would have to write. It
 * holds the logs of the start until [OpenTelemetryLogsAutoConfiguration] hands it the SDK, then sends them.
 */
internal class OpenTelemetryLogsListener : SmartApplicationListener {

    override fun supportsEventType(eventType: Class<out ApplicationEvent>): Boolean =
        ApplicationEnvironmentPreparedEvent::class.java.isAssignableFrom(eventType) ||
            ContextClosedEvent::class.java.isAssignableFrom(eventType)

    override fun onApplicationEvent(event: ApplicationEvent) {
        val root = (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(Logger.ROOT_LOGGER_NAME)

        when (event) {
            is ApplicationEnvironmentPreparedEvent -> {
                val exported = event.environment.getProperty<Boolean>("management.logging.export.otlp.enabled", true)
                if (exported && root.getAppender(NAME) == null) {
                    root.addAppender(appender(root.loggerContext))
                }
            }

            is ContextClosedEvent -> root.getAppender(NAME)?.let { appender ->
                root.detachAppender(appender)
                appender.stop()
            }
        }
    }

    // Right after Spring Boot sets Logback up, which drops any appender added before
    override fun getOrder(): Int = LoggingApplicationListener.DEFAULT_ORDER + 1

    private fun appender(context: LoggerContext) = OpenTelemetryAppender().apply {
        this.context = context
        name = NAME
        addFilter(ExporterLogs())
        start()
    }

    // The exporter logs its own failures: sent back to it, a platform that is down would get one more log per export
    private class ExporterLogs : Filter<ILoggingEvent>() {
        override fun decide(event: ILoggingEvent): FilterReply =
            if (event.loggerName.startsWith("io.opentelemetry")) FilterReply.DENY else FilterReply.NEUTRAL
    }

    private companion object {
        const val NAME = "OPENTELEMETRY"
    }
}
