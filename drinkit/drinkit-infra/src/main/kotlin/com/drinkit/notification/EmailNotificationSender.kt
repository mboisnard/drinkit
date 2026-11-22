package com.drinkit.notification

import com.drinkit.i18n.Translations
import com.drinkit.mail.Content
import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.mail.Recipients
import com.drinkit.mail.Sender
import com.drinkit.mail.Subject
import com.drinkit.notification.core.Notification
import com.drinkit.notification.spi.NotificationSender
import org.springframework.stereotype.Service

@Service
internal class EmailNotificationSender(private val emailSender: EmailSender, private val translations: Translations) :
    NotificationSender {

    // Translated last, so that no queue before this one carries the text
    override fun queue(notification: Notification) = with(notification) {
        val email = Email(
            sender = Sender("contact@drinkit.com"),
            recipients = Recipients(setOf(Recipients.Recipient(recipient.value))),
            subject = Subject(translate(content.titleKey)),
            content = Content(Content.TextFormat.TXT, translate(content.contentKey)),
        )

        emailSender.send(email)
    }

    private fun Notification.translate(key: String) = translations.translate(key, locale, content.values)
}
