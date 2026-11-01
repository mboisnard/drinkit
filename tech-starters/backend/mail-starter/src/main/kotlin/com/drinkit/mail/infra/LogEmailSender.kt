package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging

// The email holds its recipients and, for a verification, its code: only a developer's machine logs it, at DEBUG
internal class LogEmailSender : EmailSender {

    private val logger = KotlinLogging.logger { }

    init {
        logger.warn { "Emails are written to the logs and never sent: select the SMTP sender outside development" }
    }

    override fun send(email: Email) {
        logger.debug { "Log email sender: $email" }
    }
}
