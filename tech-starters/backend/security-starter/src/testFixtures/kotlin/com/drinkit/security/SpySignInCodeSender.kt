package com.drinkit.security

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps the last code sent to each login, so that a test signs in with it as a user would with their email. A login
 * given to [failFor] makes the sending throw, as an unreachable mail server would.
 */
class SpySignInCodeSender : SignInCodeSender {

    private val codes = ConcurrentHashMap<String, String>()
    private val failingLogins = ConcurrentHashMap.newKeySet<String>()

    override fun send(login: String, code: String, locale: Locale) {
        check(login !in failingLogins) { "The code could not be sent" }
        codes[login] = code
    }

    fun failFor(login: String) {
        failingLogins += login
    }

    fun lastCodeOf(login: String): String = requireNotNull(codes[login]) { "No code sent to this login" }

    fun hasSentTo(login: String) = codes.containsKey(login)
}
