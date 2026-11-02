package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging

internal class LogEmailSender : EmailSender {

    private val logger = KotlinLogging.logger { }

    init {
        logger.warn { "No mail server in spring.mail.host: emails are written to the logs at DEBUG, never sent" }
    }

    override fun send(email: Email) {
        logger.debug { "Log email sender: $email" }
    }
}
