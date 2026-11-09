package com.drinkit.mail.infra

import com.drinkit.messaging.PlatformEventHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

internal class SmtpEmailDelivery(private val mailSender: JavaMailSender) {

    private val logger = KotlinLogging.logger { }

    @PlatformEventHandler("mail.delivery.queue", idempotent = true)
    fun deliver(command: SendEmail) {
        val email = command.email
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setFrom(email.sender.value)
        helper.setTo(email.recipients.addresses().toTypedArray())
        helper.setSubject(email.subject.value)
        helper.setText(email.content.value, email.content.isHtmlFormat)

        try {
            mailSender.send(message)
        } catch (ex: MailException) {
            logger.warn(ex) { "Sending the email \"${email.subject.value}\" failed" }
            throw ex
        }
    }
}
