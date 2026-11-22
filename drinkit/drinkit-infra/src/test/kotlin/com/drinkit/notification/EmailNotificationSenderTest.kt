package com.drinkit.notification

import com.drinkit.i18n.catalogTranslations
import com.drinkit.mail.SpyEmailSender
import com.drinkit.notification.core.Notification
import com.drinkit.notification.core.NotificationContent
import com.drinkit.notification.core.Recipient
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Locale

internal class EmailNotificationSenderTest {

    private val emailSender = SpyEmailSender()
    private val notifications = EmailNotificationSender(emailSender, catalogTranslations(prefixes = setOf("mail")))

    @ParameterizedTest
    @CsvSource(
        "en, Your DrinkIt sign-in code",
        "fr, Votre code de connexion à DrinkIt",
        "it, Your DrinkIt sign-in code",
    )
    fun `an email reads in the language of its notification, English when the catalog lacks it`(
        language: String,
        subject: String,
    ) {
        // When
        notifications.queue(aSignInCodeNotification(Locale.forLanguageTag(language)))

        // Then
        emailSender.findLastSentEmail().shouldNotBeNull().subject.value shouldBe subject
    }

    @Test
    fun `an email places the values of its notification`() {
        // When
        notifications.queue(aSignInCodeNotification(Locale.ENGLISH))

        // Then
        emailSender.findLastSentEmail().shouldNotBeNull().content.value shouldContain "042137"
    }

    private fun aSignInCodeNotification(locale: Locale) = Notification(
        content = NotificationContent("mail.signInCode.subject", "mail.signInCode.body", mapOf("code" to "042137")),
        locale = locale,
        recipient = Recipient("ada@drinkit.test"),
    )
}
