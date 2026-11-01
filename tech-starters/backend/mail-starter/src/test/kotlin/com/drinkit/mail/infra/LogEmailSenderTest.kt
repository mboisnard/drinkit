package com.drinkit.mail.infra

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.drinkit.mail.Content
import com.drinkit.mail.Content.TextFormat.TXT
import com.drinkit.mail.Email
import com.drinkit.mail.Recipients
import com.drinkit.mail.Recipients.Recipient
import com.drinkit.mail.Subject
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
internal class LogEmailSenderTest {

    private val email = Email(
        recipients = Recipients(setOf(Recipient(RECIPIENT))),
        subject = Subject("Your verification code"),
        content = Content(TXT, "Your code is 731904"),
    )

    @Test
    fun `writes the whole email when its logger is at debug level`(output: CapturedOutput) {
        val sender = LogEmailSender()

        // When
        withLevel(Level.DEBUG) { sender.send(email) }

        // Then
        output.out shouldContain RECIPIENT
    }

    @Test
    fun `writes nothing of the email when its logger is at info level`(output: CapturedOutput) {
        val sender = LogEmailSender()

        // When
        withLevel(Level.INFO) { sender.send(email) }

        // Then
        output.out shouldNotContain RECIPIENT
    }

    private fun withLevel(level: Level, block: () -> Unit) {
        val logger = LoggerFactory.getLogger(LogEmailSender::class.java) as Logger
        val previous = logger.level
        logger.level = level
        try {
            block()
        } finally {
            logger.level = previous
        }
    }

    companion object {
        private const val RECIPIENT = "jane.doe@example.com"
    }
}
