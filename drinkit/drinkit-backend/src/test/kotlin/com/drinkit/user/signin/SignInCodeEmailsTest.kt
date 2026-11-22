package com.drinkit.user.signin

import com.drinkit.notification.core.Recipient
import com.drinkit.notification.spi.SpyNotificationSender
import com.drinkit.security.SignInCodeSender
import com.drinkit.security.SignInCodeSenderTestContract
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SignInCodeEmailsTest : SignInCodeSenderTestContract() {

    private val notifications = SpyNotificationSender()
    private val emails = SignInCodeEmails(notifications)

    override fun fetchSignInCodeSender(): SignInCodeSender = emails

    override fun codeSentTo(login: String) = notifications.lastSent()
        ?.takeIf { it.recipient == Recipient(login) }
        ?.content?.content?.let { Regex("\\d{6}").find(it)?.value }

    @Test
    fun `emails the code to the address that asked for it`() {
        // When
        emails.send("ada@drinkit.test", "042137", Locale.FRENCH)

        // Then
        notifications.lastSent().shouldNotBeNull() should {
            it.recipient shouldBe Recipient("ada@drinkit.test")
            it.content.shouldNotBeNull().content shouldContain "042137"
        }
    }
}
