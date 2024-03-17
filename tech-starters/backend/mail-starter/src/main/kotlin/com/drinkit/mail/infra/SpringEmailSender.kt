package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["email.sender.enabled"], havingValue = "true")
internal class SpringEmailSender(
    private val emailSender: JavaMailSender,
) : EmailSender {

    private val logger = KotlinLogging.logger { }

    override fun send(email: Email) {
        logger.debug { "Sending email $email" }

        val message = emailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setFrom(email.sender.value)
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
            logger.debug { "Email sent to ${email.recipients}, $message" }
        } catch (ex: MailException) {
            logger.error(ex) { "Error while sending email $email" }
        }
    }
}
