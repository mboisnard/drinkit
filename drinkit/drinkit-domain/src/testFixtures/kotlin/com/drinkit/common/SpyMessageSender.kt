package com.drinkit.common

class SpyMessageSender : MessageSender {

    private var sentMessages: List<SendMessageCommand> = mutableListOf()

    override fun send(command: SendMessageCommand): Boolean {
        sentMessages += command
        return true
    }

    fun count(): Int = sentMessages.size
}