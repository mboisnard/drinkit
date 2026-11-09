package com.drinkit.mail.infra

import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import com.drinkit.messaging.SpyPlatformEventPublisher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class QueuedEmailSenderTest {

    private val publisher = SpyPlatformEventPublisher()

    @Test
    fun `an email sent is published as an event instead of delivered`() {
        // Given
        val email = Email(
            sender = Sender("contact@drinkit.com"),
            recipients = Recipients(setOf(Recipient("jane.doe@example.com"))),
            subject = Subject("Your verification code"),
            content = Content(TXT, "Your code is 731904"),
        )

        // When
        QueuedEmailSender(publisher).send(email)

        // Then
        publisher.findLastSentEvent().shouldBeInstanceOf<SendEmail>().email shouldBe email
    }
}
