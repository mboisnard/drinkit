package com.drinkit.mail.infra

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

internal class MailDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val resource = ClassPathResource("META-INF/drinkit/mail-defaults.yml")
        if (!resource.exists()) return

        // addLast gives the lowest precedence: application.yml, env vars and CLI args all override these
        YamlPropertySourceLoader()
            .load("mail-starter-defaults", resource)
            .forEach { environment.propertySources.addLast(it) }
    }
}
