package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.mail.Recipients.Recipient

class SpyEmailSender : EmailSender {

    private var sentEmails: List<Email> = emptyList()

    override fun send(email: Email) {
        sentEmails += email
    }

    fun count(): Int = sentEmails.size

    fun countForRecipient(recipient: Recipient): Int =
        sentEmails.count { it.recipients.values.contains(recipient) }

    fun findLastSentEmail(): Email? = sentEmails.lastOrNull()
}