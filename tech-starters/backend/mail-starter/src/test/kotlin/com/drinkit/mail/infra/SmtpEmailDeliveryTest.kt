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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
internal class SmtpEmailDeliveryTest {

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
    fun `delivers the sender, the recipients, the subject and the content of the email`(mailpit: Mailpit) {
        // When
        SmtpEmailDelivery(mailpit.javaMailSender()).deliver(SendEmail(email))

        // Then
        val delivered = mailpit.lastEmail()
        delivered.from.map { it.toString() } shouldContainExactly listOf(SENDER)
        delivered.getRecipients(TO).map { it.toString() } shouldContainExactly listOf(RECIPIENT)
        delivered.subject shouldBe "Your verification code"
        ByteArrayOutputStream().also { delivered.writeTo(it) }.toString() shouldContain "Your code is $CODE"
    }

    @Test
    fun `a refused delivery fails, so that it is attempted again`() {
        // When & Then
        shouldThrow<MailSendException> { SmtpEmailDelivery(refusingServer).deliver(SendEmail(email)) }
    }

    @Test
    fun `a refused delivery is logged with the subject and the failure, never the content`(output: CapturedOutput) {
        // When
        runCatching { SmtpEmailDelivery(refusingServer).deliver(SendEmail(email)) }

        // Then
        output.out shouldContain "Sending the email \"Your verification code\" failed"
        output.out shouldContain "MailSendException: 451 Try again later"
        output.out shouldNotContain CODE
    }

    companion object {
        private const val SENDER = "contact@drinkit.com"
        private const val RECIPIENT = "jane.doe@example.com"
        private const val CODE = "731904"
    }
}
