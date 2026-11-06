package com.drinkit.mail.infra

import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.Mailpit
import com.drinkit.mail.MailpitIntegrationTest
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import com.drinkit.messaging.SpyPlatformEventPublisher
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import jakarta.mail.Message.RecipientType.TO
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.io.ByteArrayOutputStream

@MailpitIntegrationTest
@ExtendWith(OutputCaptureExtension::class)
internal class SpringEmailSenderTest {

    private val publisher = SpyPlatformEventPublisher()

    private val email = Email(
        sender = Sender(SENDER),
        recipients = Recipients(setOf(Recipient(RECIPIENT))),
        subject = Subject("Your verification code"),
        content = Content(TXT, "Your code is $CODE"),
    )

    private val refusingServer = object : JavaMailSenderImpl() {
        override fun doSend(mimeMessages: Array<MimeMessage>, originalMessages: Array<out Any>?): Unit =
            throw MailSendException("451 Try again later")
    }

    @Test
    fun `queues the email instead of delivering it`() {
        // When
        SpringEmailSender(publisher, refusingServer).send(email)

        // Then
        publisher.findLastSentEvent().shouldBeInstanceOf<SendEmail>().email shouldBe email
    }

    @Test
    fun `delivers a queued email to the mail server`(mailpit: Mailpit) {
        // When
        SpringEmailSender(publisher, mailpit.javaMailSender()).deliver(SendEmail(email))

        // Then
        val delivered = mailpit.lastEmail()
        delivered.from.map { it.toString() } shouldContainExactly listOf(SENDER)
        delivered.getRecipients(TO).map { it.toString() } shouldContainExactly listOf(RECIPIENT)
        delivered.subject shouldBe "Your verification code"
        ByteArrayOutputStream().also { delivered.writeTo(it) }.toString() shouldContain "Your code is $CODE"
    }

    @Test
    fun `fails a refused delivery, so that it is attempted again`() {
        shouldThrow<MailSendException> { SpringEmailSender(publisher, refusingServer).deliver(SendEmail(email)) }
    }

    @Test
    fun `logs a refused delivery with the subject of the email and the failure, never its content`(
        output: CapturedOutput,
    ) {
        // When
        runCatching { SpringEmailSender(publisher, refusingServer).deliver(SendEmail(email)) }

        // Then
        output.out shouldContain "Sending the email \"Your verification code\" failed"
        output.out shouldContain "MailSendException: 451 Try again later"
        output.out shouldNotContain CODE
    }

    @Test
    fun `never prints the content of an email to send`() {
        SendEmail(email).toString() shouldNotContain CODE
    }

    companion object {
        private const val SENDER = "contact@drinkit.com"
        private const val RECIPIENT = "jane.doe@example.com"
        private const val CODE = "731904"
    }
}
