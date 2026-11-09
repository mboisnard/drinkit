package com.drinkit.mail.infra

import com.drinkit.mail.EmailSender
import com.drinkit.mail.SpyEmailSender
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.NestedExceptionUtils.getMostSpecificCause
import org.springframework.mail.javamail.JavaMailSenderImpl

internal class MailAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(MailApplication::class.java)
        .withInitializer {
            MailDefaultsEnvironmentPostProcessor().postProcessEnvironment(it.environment, SpringApplication())
        }

    @Test
    fun `with a mail server set, emails go over SMTP on port 587 with STARTTLS by default`() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com")
            .run { context ->
                // When & Then
                context.getBean<EmailSender>().shouldBeInstanceOf<QueuedEmailSender>()
                context.getBean<SmtpEmailDelivery>()
                val mailServer = context.getBean<JavaMailSenderImpl>()
                mailServer.port shouldBe 587
                mailServer.javaMailProperties["mail.smtp.starttls.enable"] shouldBe "true"
            }
    }

    @Test
    fun `the application's own port wins over the default of the starter`() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com", "spring.mail.port=2525")
            .run { context ->
                // When & Then
                context.getBean<JavaMailSenderImpl>().port shouldBe 2525
            }
    }

    @Test
    fun `with a mail server but without messaging, the application refuses to start`() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com", "starters.messaging.enabled=false")
            .run { context ->
                // When & Then
                getMostSpecificCause(context.startupFailure.shouldNotBeNull()).message shouldBe
                    "The mail starter delivers through the messaging starter: keep starters.messaging.enabled"
            }
    }

    @Test
    fun `without a mail server, emails are dropped instead of sent`() {
        runner.run { context ->
            // When & Then
            context.getBean<EmailSender>().shouldBeInstanceOf<NoopEmailSender>()
        }
    }

    @Test
    fun `backs off when the application declares its own sender`() {
        runner.withBean(EmailSender::class.java, { SpyEmailSender() })
            .run { context ->
                // When & Then
                context.getBean<EmailSender>().shouldBeInstanceOf<SpyEmailSender>()
            }
    }

    @Test
    fun `registers no delivery when the application declares its own sender`() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com")
            .withBean(EmailSender::class.java, { SpyEmailSender() })
            .run { context ->
                // When & Then
                context.getBeanNamesForType(SmtpEmailDelivery::class.java).size shouldBe 0
            }
    }

    @Test
    fun `registers no sender when disabled`() {
        runner.withPropertyValues("starters.mail.enabled=false")
            .run { context ->
                // When & Then
                context.getBeanNamesForType(EmailSender::class.java).size shouldBe 0
            }
    }
}
