package com.drinkit.message.sender

import com.drinkit.common.MessageSender
import com.drinkit.common.SendMessageCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
internal class LoggerMessageSender: MessageSender {

    private val logger = KotlinLogging.logger { }

    override fun send(command: SendMessageCommand): Boolean {
        logger.info { "$command" }
        return true
    }
}