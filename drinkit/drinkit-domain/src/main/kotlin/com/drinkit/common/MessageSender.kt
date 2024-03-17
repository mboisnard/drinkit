package com.drinkit.common

import java.util.Locale

data class SendMessageCommand(
    val content: MessageContent? = null,
    val templateLocation: String? = null,
    val locale: Locale,
    val recipient: Recipient,
) {
    init {
        require(content != null || templateLocation != null)
    }
}

data class Recipient(
    val value: String,
)

data class MessageContent(
    val title: String,
    val content: String,
)

fun interface MessageSender {

    fun send(command: SendMessageCommand): Boolean
}
