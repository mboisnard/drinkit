package com.drinkit.mail.infra

import com.drinkit.mail.Sender
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "starters.mail")
internal data class MailProperties(
    val enabled: Boolean = true,
    val sender: SenderType = SenderType.SMTP,
    val from: Sender = Sender("contact@drinkit.com"),
) {

    enum class SenderType { LOGGING, SMTP }
}
