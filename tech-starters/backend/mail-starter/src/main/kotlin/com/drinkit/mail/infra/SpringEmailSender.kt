package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.mail.Sender
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

internal class SpringEmailSender(private val emailSender: JavaMailSender?, private val from: Sender) : EmailSender {

    private val logger = KotlinLogging.logger { }

    // The email and the message of a failure hold the recipients and, for a verification, its code
    override fun send(email: Email) {
        val emailSender = emailSender ?: return logger.error { "No mail server configured, an email was not sent" }

        val message = emailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setFrom((email.sender ?: from).value)
        helper.setTo(email.recipients.allAsString().toTypedArray())
        helper.setSubject(email.subject.value)
        helper.setText(email.content.value, email.content.isHtmlFormat)

        /*
        if (email.hasAttachments) {
            email.attachments.values.map {
                val file = File("")
                helper.addAttachment(it.title, file)
            }
        }
         */

        try {
            emailSender.send(message)
        } catch (ex: MailException) {
            logger.error { "Sending an email failed with ${ex.javaClass.simpleName}" }
        }
    }
}
