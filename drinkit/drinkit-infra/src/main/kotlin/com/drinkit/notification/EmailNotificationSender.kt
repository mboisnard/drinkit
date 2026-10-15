package com.drinkit.notification

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
internal class EmailNotificationSender(private val emailSender: EmailSender) : NotificationSender {

    override fun send(notification: Notification): Boolean = with(notification) {
        val body = checkNotNull(content) { "An email is sent from a content, not from a template" }
        val email = Email(
            sender = Sender("contact@drinkit.com"),
            recipients = Recipients(setOf(Recipients.Recipient(recipient.value))),
            subject = Subject(body.title),
            content = Content(format = Content.TextFormat.TXT, value = body.content),
        )

        emailSender.send(email)
        true
    }
}
