package com.drinkit.mail.infra

import com.drinkit.mail.EmailSender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.mail.javamail.JavaMailSenderImpl

internal class MailAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(MailSenderAutoConfiguration::class.java, MailAutoConfiguration::class.java),
        )

    @Test
    fun `sends through the mail server of spring mail host, on port 587 with STARTTLS`() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com")
            .run { context ->
                context.getBean<EmailSender>().shouldBeInstanceOf<SpringEmailSender>()
                val mailServer = context.getBean<JavaMailSenderImpl>()
                mailServer.port shouldBe 587
                mailServer.javaMailProperties["mail.smtp.starttls.enable"] shouldBe "true"
            }
    }

    @Test
    fun `lets the application override a default of the starter`() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com", "spring.mail.port=2525")
            .run { context ->
                context.getBean<JavaMailSenderImpl>().port shouldBe 2525
            }
    }

    @Test
    fun `writes the emails to the logs without a mail server`() {
        runner.run { context ->
            context.getBean<EmailSender>().shouldBeInstanceOf<LogEmailSender>()
        }
    }

    @Test
    fun `backs off when the application declares its own sender`() {
        runner.withBean(EmailSender::class.java, { SpyEmailSender() })
            .run { context ->
                context.getBean<EmailSender>().shouldBeInstanceOf<SpyEmailSender>()
            }
    }

    @Test
    fun `registers no sender when disabled`() {
        runner.withPropertyValues("starters.mail.enabled=false")
            .run { context ->
                context.getBeanNamesForType(EmailSender::class.java).size shouldBe 0
            }
    }
}
