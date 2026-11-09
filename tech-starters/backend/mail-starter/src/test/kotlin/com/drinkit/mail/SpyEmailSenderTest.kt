package com.drinkit.mail

internal class SpyEmailSenderTest : EmailSenderTestContract() {

    private val sender = SpyEmailSender()

    override fun sender(): EmailSender = sender

    override fun deliveredCount(email: Email): Int = sender.countForRecipient(email.recipients.values.first())
}
