package com.drinkit.mail

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
fun interface EmailSender {

    /** Queues the email, after the commit inside a transaction, and returns without waiting for the mail server. */
    fun send(email: Email)
}
