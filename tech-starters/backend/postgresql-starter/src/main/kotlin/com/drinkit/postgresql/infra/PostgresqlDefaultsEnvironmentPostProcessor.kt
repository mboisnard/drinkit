package com.drinkit.postgresql.infra

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.getProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.ResourcePropertySource

// Not a @PropertySource: the logging levels are read before any configuration class
internal class PostgresqlDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        if (!environment.getProperty<Boolean>("starters.postgresql.enabled", true)) {
            return
        }

        // Last, below every property source of the application
        environment.propertySources.addLast(
            ResourcePropertySource(
                "postgresql-starter-defaults",
                ClassPathResource("META-INF/drinkit/postgresql-defaults.properties"),
            ),
        )

        // Without an application name, Hikari and the driver keep their own names
        environment.getProperty("spring.application.name")?.let { name ->
            environment.propertySources.addLast(
                MapPropertySource(
                    "postgresql-starter-names",
                    mapOf(
                        "spring.datasource.hikari.pool-name" to name,
                        "spring.datasource.hikari.data-source-properties.ApplicationName" to name,
                    ),
                ),
            )
        }
    }
}
