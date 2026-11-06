package com.drinkit.security.infra.code

import com.drinkit.security.SignInCodeSender
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.hashOf
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.SecureRandom
import java.time.Clock
import java.util.Locale

private const val CODE_DIGITS = 6
private const val CODE_BOUND = 1_000_000

/**
 * Never looks the login up, so that the answer is the same, and as fast, whether an account exists or not.
 *
 * OWASP: ASVS 5.0 V6.3.8, V11.5.1, Authentication Cheat Sheet (same answer for any account, random codes).
 */
internal class SignInCodeIssuer(
    private val codes: SignInCodes,
    private val sender: SignInCodeSender,
    private val clock: Clock,
    private val settings: SecurityStarterProperties.Code,
) {

    private val logger = KotlinLogging.logger {}
    private val random = SecureRandom()

    fun issue(login: String, locale: Locale) {
        val now = clock.instant()
        codes.deleteExpiredAt(now)

        val code = random.nextInt(CODE_BOUND).toString().padStart(CODE_DIGITS, '0')
        codes.replace(
            SignInCode(
                login,
                hashOf(code),
                now + settings.validity,
                settings.maxAttempts,
                now + settings.attemptsWindow,
            ),
            now,
        )

        // Any failure of the application's sender, which the answer must not reveal
        @Suppress("TooGenericExceptionCaught")
        try {
            sender.send(login, code, locale)
        } catch (exception: RuntimeException) {
            logger.warn { "Sign-in code not sent: ${exception.javaClass.simpleName}" }
        }
    }
}
