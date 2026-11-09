package com.drinkit.mail.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

internal class MailDefaultsEnvironmentPostProcessorTest {

    @Test
    fun `an SMTP server is reached on port 587 with STARTTLS by default`() {
        // When
        val environment = withDefaults()

        // Then
        environment.getProperty("spring.mail.port") shouldBe "587"
        environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable") shouldBe "true"
    }

    @Test
    fun `the application's own port wins`() {
        // When
        val environment = withDefaults("spring.mail.port" to "2525")

        // Then
        environment.getProperty("spring.mail.port") shouldBe "2525"
    }

    @Test
    fun `adds no default when the starter is disabled`() {
        // When
        val environment = withDefaults("starters.mail.enabled" to "false")

        // Then
        environment.getProperty("spring.mail.port") shouldBe null
    }

    private fun withDefaults(vararg applicationProperties: Pair<String, String>): StandardEnvironment {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("application", mapOf(*applicationProperties)))
        MailDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())
        return environment
    }
}
