package com.drinkit.mail.infra

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.getProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.ResourcePropertySource

internal class MailDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        if (!environment.getProperty<Boolean>("starters.mail.enabled", true)) {
            return
        }

        // Last, below every property source of the application
        environment.propertySources.addLast(
            ResourcePropertySource(
                "mail-starter-defaults",
                ClassPathResource("META-INF/drinkit/mail-defaults.properties"),
            ),
        )
    }
}
