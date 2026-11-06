package com.drinkit.user.signin

import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.notification.core.Recipient
import com.drinkit.notification.spi.SpyNotificationSender
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SignInCodeEmailsTest {

    private val publisher = SpyPlatformEventPublisher()
    private val notifications = SpyNotificationSender()
    private val emails = SignInCodeEmails(publisher, notifications)

    @Test
    fun `sends nothing during the request, only asks for the code to be emailed after it`() {
        // When
        emails.send("ada@drinkit.test", "042137", Locale.FRENCH)

        // Then
        notifications.count() shouldBe 0
        publisher.findLastSentEvent().shouldBeInstanceOf<SignInCodeRequested>() shouldBe
            SignInCodeRequested("ada@drinkit.test", "042137", Locale.FRENCH)
    }

    @Test
    fun `emails the code to the address that asked for it`() {
        // When
        emails.sendTheCode(SignInCodeRequested("ada@drinkit.test", "042137", Locale.FRENCH))

        // Then
        notifications.lastSent().shouldNotBeNull() should {
            it.recipient shouldBe Recipient("ada@drinkit.test")
            it.content.shouldNotBeNull().content shouldContain "042137"
        }
    }

    @Test
    fun `keeps the email and the code out of the text of the event`() {
        SignInCodeRequested("ada@drinkit.test", "042137", Locale.FRENCH).toString() should {
            it shouldNotContain "ada@drinkit.test"
            it shouldNotContain "042137"
        }
    }
}
