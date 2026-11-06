package com.drinkit.security

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
