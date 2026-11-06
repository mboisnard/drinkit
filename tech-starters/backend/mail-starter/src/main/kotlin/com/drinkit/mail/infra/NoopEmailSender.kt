package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging

internal class NoopEmailSender : EmailSender {

    private val logger = KotlinLogging.logger { }

    init {
        logger.warn { "No mail server in spring.mail.host: emails are not sent" }
    }

    override fun send(email: Email) {
        logger.warn { "The email \"${email.subject.value}\" was not sent: no mail server in spring.mail.host" }
    }
}
