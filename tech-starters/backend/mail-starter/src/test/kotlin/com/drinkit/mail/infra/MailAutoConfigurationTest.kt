package com.drinkit.mail.infra

import com.drinkit.mail.EmailSender
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class MailAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(MailSenderAutoConfiguration::class.java, MailAutoConfiguration::class.java),
        )

    @Test
    fun `sends through SMTP by default, even without a mail server`() {
        runner.run { context ->
            context.getBean<EmailSender>().shouldBeInstanceOf<SpringEmailSender>()
        }
    }

    @Test
    fun `refuses to start on a From address that is not an email address`() {
        runner.withPropertyValues("starters.mail.from=contact")
            .run { context ->
                context.startupFailure.shouldNotBeNull().stackTraceToString() shouldContain
                    "Failed to bind properties under 'starters.mail.from'"
            }
    }

    @Test
    fun `selects the logging sender`() {
        runner.withPropertyValues("starters.mail.sender=logging")
            .run { context ->
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
