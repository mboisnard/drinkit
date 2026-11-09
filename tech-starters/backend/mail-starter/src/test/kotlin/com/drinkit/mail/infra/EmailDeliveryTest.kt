package com.drinkit.mail.infra

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.mail.Mailpit
import com.drinkit.mail.MailpitIntegrationTest
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@MailpitIntegrationTest
@ExtendWith(OutputCaptureExtension::class)
internal class EmailDeliveryTest {

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(MailApplication::class.java)
        .withPropertyValues("starters.messaging.retry.delay=1s", "starters.messaging.retry.exponential-backoff=false")

    @Test
    fun `returns before a slow mail server answers`() {
        val slowServer = object : JavaMailSenderImpl() {
            override fun doSend(mimeMessages: Array<MimeMessage>, originalMessages: Array<out Any>?) {
                Thread.sleep(5_000)
            }
        }

        runner.withBean(JavaMailSender::class.java, { slowServer }).run { context ->
            // When
            val elapsed = measureTime { context.getBean<EmailSender>().send(emailWithSubject("Slow server")) }

            // Then
            elapsed shouldBeLessThan 1.seconds
        }
    }

    @Test
    fun `delivers on a later attempt an email the server first refused with a temporary error`(
        mailpit: Mailpit,
        output: CapturedOutput,
    ) {
        mailpit.refuseRecipients(451)

        runner.withBean(JavaMailSender::class.java, { mailpit.javaMailSender() }).run { context ->
            // When
            context.getBean<EmailSender>().send(emailWithSubject("Delivered on a later attempt"))
            runBlocking {
                eventually(5.seconds) { output.out shouldContain "Attempt 1 of handler mail.delivery.queue failed" }
            }
            mailpit.acceptAll()

            // Then
            runBlocking {
                eventually(5.seconds) { mailpit.lastEmail().subject shouldBe "Delivered on a later attempt" }
            }
        }
    }

    @Test
    fun `an email sent again by a replayed request is delivered once`(mailpit: Mailpit) {
        runner.withBean(JavaMailSender::class.java, { mailpit.javaMailSender() }).run { context ->
            // Given
            val sender = context.getBean<EmailSender>()
            CurrentIdempotencyKey.with("sign-in request") { sender.send(emailWithSubject("Sent once")) }
            runBlocking { eventually(5.seconds) { mailpit.countEmailsWithSubject("Sent once") shouldBe 1 } }

            // When
            CurrentIdempotencyKey.with("sign-in request") { sender.send(emailWithSubject("Sent once")) }

            // Then
            runBlocking { continually(2.seconds) { mailpit.countEmailsWithSubject("Sent once") shouldBe 1 } }
        }
    }

    @Test
    fun `logs an error once an email is still refused after the last attempt, never its content`(
        mailpit: Mailpit,
        output: CapturedOutput,
    ) {
        mailpit.refuseRecipients(451)

        runner.withBean(JavaMailSender::class.java, { mailpit.javaMailSender() }).run { context ->
            // When
            context.getBean<EmailSender>().send(emailWithSubject("Never delivered"))

            // Then
            runBlocking {
                eventually(10.seconds) {
                    output.out shouldContain "Handler mail.delivery.queue gave up after 3 attempts"
                }
            }
            output.out shouldNotContain CONTENT
        }
    }

    private fun emailWithSubject(subject: String) = Email(
        sender = Sender("contact@drinkit.com"),
        recipients = Recipients(setOf(Recipient("jane.doe@example.com"))),
        subject = Subject(subject),
        content = Content(TXT, CONTENT),
    )

    companion object {
        private const val CONTENT = "Your code is 731904"
    }
}
