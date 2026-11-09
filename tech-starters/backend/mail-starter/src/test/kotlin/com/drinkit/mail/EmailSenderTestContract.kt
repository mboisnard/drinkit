package com.drinkit.mail

import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Recipients.Recipient
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal abstract class EmailSenderTestContract {

    abstract fun sender(): EmailSender

    abstract fun deliveredCount(email: Email): Int

    @Test
    fun `an email sent is delivered`() {
        // Given
        val email = uniqueEmail()

        // When
        sender().send(email)

        // Then
        awaitDelivered(email, times = 1)
    }

    @Test
    fun `two equal emails sent are both delivered`() {
        // Given
        val email = uniqueEmail()

        // When
        sender().send(email)
        sender().send(email)

        // Then
        awaitDelivered(email, times = 2)
    }

    private fun awaitDelivered(email: Email, times: Int) {
        runBlocking { eventually(10.seconds) { deliveredCount(email) shouldBe times } }
    }

    private fun uniqueEmail(): Email {
        val id = UUID.randomUUID()
        return Email(
            sender = Sender("contact@drinkit.com"),
            recipients = Recipients(setOf(Recipient("$id@example.com"))),
            subject = Subject("Contract $id"),
            content = Content(TXT, "Your code is 731904"),
        )
    }
}
