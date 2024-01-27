package com.drinkit.common

import java.util.Locale

data class SendMessageCommand(
    val content: MessageContent?,
    val templateLocation: String?,
    val locale: Locale,
) {
    init {
        require(content != null || templateLocation != null)
    }
}

data class MessageContent(
    val title: String,
    val content: String,
)

fun interface MessageSender {

    fun send(command: SendMessageCommand): Boolean
}