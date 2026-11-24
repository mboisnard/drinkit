package com.drinkit.featuremanagement.infra.flipt

import dev.openfeature.contrib.providers.flipt.FliptProvider
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

// FliptProvider logs each failed evaluation as an error with its stack trace, a missing flag included:
// DefaultValueWarning says it in one line instead
internal class FliptLoggingEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        // Last, below every property source of the application
        environment.propertySources.addLast(
            MapPropertySource(
                "feature-management-starter-logging",
                mapOf("logging.level.${FliptProvider::class.java.name}" to "off"),
            ),
        )
    }
}
