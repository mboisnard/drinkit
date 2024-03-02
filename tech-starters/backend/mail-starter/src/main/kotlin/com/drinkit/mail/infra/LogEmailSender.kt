package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev")
@ConditionalOnProperty(name = ["email.sender.enabled"], havingValue = "false")
internal class LogEmailSender : EmailSender {

    private val logger = KotlinLogging.logger { }

    override fun send(email: Email) {
        logger.info { "Log email sender: $email" }
    }
}