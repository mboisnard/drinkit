package com.drinkit.security.infra

import java.security.SecureRandom
import java.util.Base64

private const val RANDOM_BYTES = 32
private val random = SecureRandom()

/**
 * 256 random bits in URL-safe Base64, too many to guess: the ids of the sessions and the remember-me tokens.
 *
 * OWASP: ASVS 5.0 V7.2.3, V11.5.1, Session Management Cheat Sheet (session id entropy).
 */
internal fun randomToken(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(RANDOM_BYTES).also(random::nextBytes))
