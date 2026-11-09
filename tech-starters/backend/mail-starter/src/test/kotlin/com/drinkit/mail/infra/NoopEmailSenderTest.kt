package com.drinkit.mail.infra

import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
internal class NoopEmailSenderTest {

    private val email = Email(
        sender = Sender("contact@drinkit.com"),
        recipients = Recipients(setOf(Recipient(RECIPIENT))),
        subject = Subject("Your verification code"),
        content = Content(TXT, "Your code is $CODE"),
    )

    @Test
    fun `warns that the email was not sent, with its subject`(output: CapturedOutput) {
        // When
        NoopEmailSender().send(email)

        // Then
        output.out shouldContain "WARN"
        output.out shouldContain "The email \"Your verification code\" was not sent"
    }

    @Test
    fun `warns at startup that emails are not sent`(output: CapturedOutput) {
        // When
        NoopEmailSender()

        // Then
        output.out shouldContain "WARN"
        output.out shouldContain "No mail server in spring.mail.host: emails are not sent"
    }

    @Test
    fun `never writes the content or the recipients of the email to the logs`(output: CapturedOutput) {
        // When
        NoopEmailSender().send(email)

        // Then
        output.out shouldContain "was not sent"
        output.out shouldNotContain CODE
        output.out shouldNotContain RECIPIENT
    }

    companion object {
        private const val RECIPIENT = "jane.doe@example.com"
        private const val CODE = "731904"
    }
}
