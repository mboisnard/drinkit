package com.drinkit.mail

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.Properties

class Mailpit(private val host: String, private val smtpPort: Int, private val apiPort: Int) {

    private val client = HttpClient.newHttpClient()

    fun javaMailSender(): JavaMailSender = JavaMailSenderImpl().also {
        it.host = host
        it.port = smtpPort
    }

    fun lastEmail(): MimeMessage {
        val raw = client.send(request("/api/v1/message/latest/raw").GET().build(), BodyHandlers.ofInputStream())
        return MimeMessage(Session.getInstance(Properties()), raw.body())
    }

    internal fun deleteEmails() {
        client.send(request("/api/v1/messages").DELETE().build(), BodyHandlers.discarding())
    }

    private fun request(path: String) = HttpRequest.newBuilder(URI("http://$host:$apiPort$path"))
}
