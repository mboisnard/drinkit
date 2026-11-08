package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.PlatformEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

internal class SpringEmailSender(
    private val publisher: PlatformEventPublisher,
    private val mailSender: JavaMailSender,
) : EmailSender {

    private val logger = KotlinLogging.logger { }

    override fun send(email: Email) {
        publisher.publish(SendEmail(email))
    }

    @PlatformEventHandler("mail.delivery.queue", idempotent = true)
    fun deliver(command: SendEmail) {
        val email = command.email
        val message = mailSender.createMimeMessage()
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
            mailSender.send(message)
        } catch (ex: MailException) {
            logger.warn(ex) { "Sending the email \"${email.subject.value}\" failed" }
            throw ex
        }
    }
}
