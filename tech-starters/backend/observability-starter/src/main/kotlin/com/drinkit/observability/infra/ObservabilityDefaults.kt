package com.drinkit.observability.infra

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.getProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.ResourcePropertySource

internal class ObservabilityDefaults : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val enabled = environment.getProperty<Boolean>("starters.observability.enabled", true)

        // Spring Boot pushes to localhost:4318 by default. Tracing itself stays on, for the traceId of the logs
        if (!enabled || !environment.getProperty<Boolean>("starters.observability.opentelemetry.enabled", false)) {
            environment.propertySources.addLast(
                MapPropertySource(
                    "observability-starter-no-export",
                    mapOf(
                        "management.otlp.metrics.export.enabled" to false,
                        "management.tracing.export.enabled" to false,
                        "management.logging.export.otlp.enabled" to false,
                    ),
                ),
            )
        }

        if (!enabled) {
            return
        }

        // Last, below every property source of the application
        environment.propertySources.addLast(
            ResourcePropertySource(
                "observability-starter-defaults",
                ClassPathResource("META-INF/drinkit/observability-defaults.properties"),
            ),
        )
    }
}
