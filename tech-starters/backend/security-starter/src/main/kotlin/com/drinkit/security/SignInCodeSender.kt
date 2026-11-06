package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.Locale

/** The port the application implements to send a sign-in code to its user, such as by email. */
@TechStarterTool
fun interface SignInCodeSender {

    /**
     * Sends [code] to [login] in [locale]. Called before the answer to the request for a code: hand the sending off,
     * such as to a platform event, so that the answer never waits for a mail server, and never log the code.
     */
    fun send(login: String, code: String, locale: Locale)
}
