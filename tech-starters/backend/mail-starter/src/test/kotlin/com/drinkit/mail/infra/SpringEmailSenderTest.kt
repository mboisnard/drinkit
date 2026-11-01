package com.drinkit.mail.infra

import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import jakarta.mail.Message.RecipientType.TO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
internal class SpringEmailSenderTest {

    private val email = Email(
        sender = Sender("contact@drinkit.com"),
        recipients = Recipients(setOf(Recipient(RECIPIENT))),
        subject = Subject("Your verification code"),
        content = Content(TXT, "Your code is $CODE"),
    )

    @Test
    fun `sends an email with its sender, recipients, subject and content`() {
        val mailSender = CapturingJavaMailSender()

        // When
        SpringEmailSender(mailSender, Sender(FROM)).send(email)

        // Then
        val message = mailSender.sent.single()
        message.from.map { it.toString() } shouldContainExactly listOf("contact@drinkit.com")
        message.getRecipients(TO).map { it.toString() } shouldContainExactly listOf(RECIPIENT)
        message.subject shouldBe "Your verification code"
        mailSender.lastSentAsText() shouldContain "Your code is $CODE"
    }

    @Test
    fun `an email that names no sender goes out from the configured address`() {
        val mailSender = CapturingJavaMailSender()

        // When
        SpringEmailSender(mailSender, Sender(FROM)).send(email.copy(sender = null))

        // Then
        mailSender.sent.single().from.map { it.toString() } shouldContainExactly listOf(FROM)
    }

    @Test
    fun `with no mail server configured, sending logs an error without the email`(output: CapturedOutput) {
        // When
        SpringEmailSender(null, Sender(FROM)).send(email)

        // Then
        output.out shouldContain "No mail server configured, an email was not sent"
        output.out shouldNotContain RECIPIENT
        output.out shouldNotContain CODE
    }

    @Test
    fun `a failed send is logged with the class of the failure, never the email`(output: CapturedOutput) {
        // When
        SpringEmailSender(CapturingJavaMailSender(failure = "Refused $RECIPIENT"), Sender(FROM)).send(email)

        // Then
        output.out shouldContain "Sending an email failed with MailSendException"
        output.out shouldNotContain RECIPIENT
        output.out shouldNotContain CODE
    }

    companion object {
        private const val RECIPIENT = "jane.doe@example.com"
        private const val CODE = "731904"
        private const val FROM = "noreply@drinkit.com"
    }
}
