package com.drinkit.mail.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

internal class MailDefaultsEnvironmentPostProcessorTest {

    private val environment = StandardEnvironment()

    @Test
    fun `gives Spring's mail settings the starter's defaults`() {
        // When
        MailDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())

        // Then
        environment.getProperty("spring.mail.port") shouldBe "587"
        environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable") shouldBe "true"
        environment.getProperty("spring.mail.properties.mail.smtp.timeout") shouldBe "5000"
    }

    @Test
    fun `the application's own value wins over a default`() {
        environment.propertySources.addFirst(MapPropertySource("application", mapOf("spring.mail.port" to "2525")))

        // When
        MailDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())

        // Then
        environment.getProperty("spring.mail.port") shouldBe "2525"
    }
}
