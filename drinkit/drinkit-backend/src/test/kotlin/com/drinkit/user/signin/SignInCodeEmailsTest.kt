package com.drinkit.user.signin

import com.drinkit.i18n.FixedCurrentLocale
import com.drinkit.notification.core.Recipient
import com.drinkit.notification.spi.SpyNotificationSender
import com.drinkit.security.SignInCodeSender
import com.drinkit.security.SignInCodeSenderTestContract
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SignInCodeEmailsTest : SignInCodeSenderTestContract() {

    private val notifications = SpyNotificationSender()
    private val emails = SignInCodeEmails(notifications, FixedCurrentLocale(Locale.FRENCH))

    override fun fetchSignInCodeSender(): SignInCodeSender = emails

    override fun codeSentTo(login: String) =
        notifications.lastSent()?.takeIf { it.recipient == Recipient(login) }?.content?.values?.get("code") as String?

    @Test
    fun `emails the code to the address that asked for it, in the language of the request`() {
        // When
        emails.send("ada@drinkit.test", "042137")

        // Then
        notifications.lastSent().shouldNotBeNull() should {
            it.recipient shouldBe Recipient("ada@drinkit.test")
            it.locale shouldBe Locale.FRENCH
            it.content.values shouldBe mapOf("code" to "042137")
        }
    }
}
