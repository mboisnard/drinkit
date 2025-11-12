package com.drinkit.mail

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
fun interface EmailSender {

    fun send(email: Email)
}
