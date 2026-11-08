package com.drinkit.mail

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.net.HttpURLConnection.HTTP_OK
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets.UTF_8
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

    fun countEmailsWithSubject(subject: String): Int {
        val query = URLEncoder.encode("subject:\"$subject\"", UTF_8)
        val page = client.send(request("/api/v1/search?query=$query").GET().build(), BodyHandlers.ofString()).body()
        return checkNotNull(MATCHES.find(page)) { "No count in Mailpit's answer: $page" }.groupValues[1].toInt()
    }

    /** Answers [code] to every recipient of the next emails, until [acceptAll]. */
    fun refuseRecipients(code: Int) = setChaos(recipientProbability = 100, code = code)

    fun acceptAll() = setChaos(recipientProbability = 0, code = 451)

    private fun setChaos(recipientProbability: Int, code: Int) {
        val trigger = """{"ErrorCode": $code, "Probability": $recipientProbability}"""
        val triggers = """{"Recipient": $trigger}"""
        val response = client.send(
            request("/api/v1/chaos").header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString(triggers)).build(),
            BodyHandlers.ofString(),
        )
        check(response.statusCode() == HTTP_OK) { "Mailpit refused the chaos triggers: ${response.body()}" }
    }

    internal fun deleteEmails() {
        client.send(request("/api/v1/messages").DELETE().build(), BodyHandlers.discarding())
    }

    private fun request(path: String) = HttpRequest.newBuilder(URI("http://$host:$apiPort$path"))

    private companion object {
        val MATCHES = Regex("\"messages_count\":(\\d+)")
    }
}
