package com.drinkit.mail.infra

import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

internal class SendEmailTest {

    @Test
    fun `the event never prints the content or the recipients of its email`() {
        // Given
        val email = Email(
            sender = Sender("contact@drinkit.com"),
            recipients = Recipients(setOf(Recipient("jane.doe@example.com"))),
            subject = Subject("Your verification code"),
            content = Content(TXT, "Your code is 731904"),
        )

        // When
        val printed = SendEmail(email).toString()

        // Then
        printed shouldNotContain "731904"
        printed shouldNotContain "jane.doe@example.com"
    }
}
