package com.drinkit.security.infra.code

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** The sign-in codes of the tests that run without PostgreSQL, in memory. */
internal class InMemorySignInCodes : SignInCodes {

    private val codes = ConcurrentHashMap<String, SignInCode>()

    override fun replace(code: SignInCode, now: Instant) {
        codes.compute(code.login) { _, earlier -> withTriesOf(code, earlier, now) }
    }

    override fun tryCode(login: String, codeHash: String, now: Instant): Boolean {
        var accepted = false

        codes.computeIfPresent(login) { _, code ->
            accepted = code.accepts(codeHash, now)
            code.afterTry(accepted)
        }

        return accepted
    }

    override fun deleteExpiredAt(now: Instant) {
        codes.values.removeIf { !it.isLiveAt(now) && !now.isBefore(it.attemptsResetAt) }
    }

    private fun withTriesOf(code: SignInCode, earlier: SignInCode?, now: Instant) =
        if (earlier != null && now.isBefore(earlier.attemptsResetAt)) {
            code.copy(attemptsLeft = earlier.attemptsLeft, attemptsResetAt = earlier.attemptsResetAt)
        } else {
            code
        }
}
