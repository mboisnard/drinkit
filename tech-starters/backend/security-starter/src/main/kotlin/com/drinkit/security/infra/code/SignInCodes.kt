package com.drinkit.security.infra.code

import com.drinkit.security.infra.matchesHash
import java.time.Instant

/**
 * The live code of [login], kept as its hash until [expiresAt], with the tries the login has left until
 * [attemptsResetAt], whatever code it holds then.
 */
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

    /** What is left of this code after a try: nothing once accepted, one try less otherwise. */
    fun afterTry(accepted: Boolean): SignInCode? =
        if (accepted) null else copy(attemptsLeft = (attemptsLeft - 1).coerceAtLeast(0))
}

/**
 * The live sign-in codes, one per login. The tries belong to the login, not to a code, and the right code is accepted
 * once. Only the hash of a code is kept: a code of six digits can still be found back from it, which is why it lives
 * minutes.
 *
 * OWASP: ASVS 5.0 V6.6, Authentication Cheat Sheet (single use, limited tries and lifetime of a code).
 */
internal interface SignInCodes {

    /** Keeps [code] as the only live code of its login, with the tries the login has left. */
    fun replace(code: SignInCode, now: Instant)

    /** True when [codeHash] is the hash of the live code of [login], which is then gone. A refusal costs a try. */
    fun tryCode(login: String, codeHash: String, now: Instant): Boolean

    fun deleteExpiredAt(now: Instant)
}
