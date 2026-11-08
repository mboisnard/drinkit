package com.drinkit.idempotency.infra

import java.time.Instant
import java.util.UUID

/** Where the keys live, PostgreSQL in production: every instance sees a key as soon as a run takes it. */
internal interface IdempotencyKeys {

    /** Takes the key unless another run holds it, forgets those taken before [forgetBefore], and returns its holder. */
    fun take(key: HeldKey, forgetBefore: Instant): HeldKey

    /**
     * Records the outcome of the run that holds [key], in the transaction open around it if any. Fails when another
     * run took the key over once the lease of this one ended: only one of them keeps its outcome.
     */
    fun complete(key: HeldKey, outcome: ByteArray?, at: Instant)

    fun release(key: HeldKey)
}

/**
 * A key and the run that holds it: its [owner], until [leaseUntil] while it runs, then for the retention once
 * completed. The [fingerprint] of the payload tells a replay from another payload sent under the same key.
 */
@Suppress("LongParameterList")
internal class HeldKey(
    val scope: String,
    val hash: String,
    val owner: UUID,
    val fingerprint: String,
    val takenAt: Instant,
    val leaseUntil: Instant,
    val completedAt: Instant? = null,
    val outcome: ByteArray? = null,
) {
    fun isCompleted() = completedAt != null

    fun completed(outcome: ByteArray?, at: Instant) =
        HeldKey(scope, hash, owner, fingerprint, takenAt, leaseUntil, completedAt = at, outcome = outcome)

    /** Free for a new run: forgotten, or left by a run that crashed with the same payload. */
    fun canBeTakenBy(other: HeldKey, forgetBefore: Instant) = takenAt < forgetBefore ||
        (!isCompleted() && leaseUntil < other.takenAt && fingerprint == other.fingerprint)
}
