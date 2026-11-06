package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.Locale

/**
 * Called before the answer to a request for a code: hand the sending off, such as to a platform event, so that the
 * answer never waits for a mail server, and never log the code.
 */
@TechStarterTool
fun interface SignInCodeSender {

    fun send(login: String, code: String, locale: Locale)
}
