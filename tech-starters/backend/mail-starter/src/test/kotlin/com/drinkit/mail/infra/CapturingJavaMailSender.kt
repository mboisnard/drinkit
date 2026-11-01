package com.drinkit.mail.infra

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.io.ByteArrayOutputStream

// Builds real MIME messages and keeps them, instead of opening an SMTP connection
internal class CapturingJavaMailSender(private val failure: String? = null) : JavaMailSenderImpl() {

    val sent = mutableListOf<MimeMessage>()

    override fun doSend(mimeMessages: Array<MimeMessage>, originalMessages: Array<out Any>?) {
        failure?.let { throw MailSendException(it) }
        sent += mimeMessages
    }

    fun lastSentAsText(): String = ByteArrayOutputStream().also { sent.last().writeTo(it) }.toString()
}
