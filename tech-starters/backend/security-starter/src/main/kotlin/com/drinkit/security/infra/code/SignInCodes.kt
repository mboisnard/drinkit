package com.drinkit.security.infra.code

import com.drinkit.security.infra.matchesHash
import java.time.Instant

internal data class SignInCode(
    val login: String,
    val codeHash: String,
    val expiresAt: Instant,
    val attemptsLeft: Int,
    val attemptsResetAt: Instant,
) {
    fun isLiveAt(now: Instant) = now.isBefore(expiresAt)

    fun accepts(codeHash: String, now: Instant) =
        isLiveAt(now) && attemptsLeft > 0 && this.codeHash.matchesHash(codeHash)

    fun afterTry(accepted: Boolean): SignInCode? =
        if (accepted) null else copy(attemptsLeft = (attemptsLeft - 1).coerceAtLeast(0))
}

/**
 * The tries belong to the login, not to a code. Six digits can be found back from their hash, which is why a code
 * lives minutes.
 *
 * OWASP: ASVS 5.0 V6.6, Authentication Cheat Sheet (single use, limited tries and lifetime of a code).
 */
internal interface SignInCodes {

    fun replace(code: SignInCode, now: Instant)

    /** An accepted code is gone, a refusal costs a try. */
    fun tryCode(login: String, codeHash: String, now: Instant): Boolean

    fun deleteExpiredAt(now: Instant)
}
