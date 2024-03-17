package com.drinkit.mail

fun interface EmailSender {

    fun send(email: Email)
}
