package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.messaging.PlatformEventPublisher

internal class QueuedEmailSender(private val publisher: PlatformEventPublisher) : EmailSender {

    override fun send(email: Email) {
        publisher.publish(SendEmail(email))
    }
}
