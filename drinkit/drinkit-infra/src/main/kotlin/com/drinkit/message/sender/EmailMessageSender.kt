package com.drinkit.message.sender

import com.drinkit.common.MessageSender
import com.drinkit.common.SendMessageCommand
import com.drinkit.mail.*
import org.springframework.stereotype.Service

@Service
internal class EmailMessageSender(
    private val emailSender: EmailSender,
) : MessageSender {
    override fun send(command: SendMessageCommand): Boolean = with(command) {

        val email = Email(
            sender = Sender("contact@drinkit.com"),
            recipients = Recipients(setOf(Recipients.Recipient(recipient.value))),
            subject = Subject(content!!.title),
            content = Content(format = Content.TextFormat.TXT, value = content!!.content),
        )

        emailSender.send(email)
        true
    }
}