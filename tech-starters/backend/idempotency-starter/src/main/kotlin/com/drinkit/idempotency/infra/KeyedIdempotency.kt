package com.drinkit.idempotency.infra

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.IdempotentResult
import com.drinkit.kotlin.sha256
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.Duration
import java.util.UUID

internal class KeyedIdempotency(
    private val keys: IdempotencyKeys,
    private val clock: Clock,
    private val properties: IdempotencyProperties,
) : Idempotency {

    override fun <T> once(
        scope: String,
        key: String,
        fingerprint: String,
        record: (T) -> ByteArray?,
        action: () -> T,
    ): IdempotentResult<T> {
        val now = clock.instant()
        val mine = HeldKey(
            scope = scope,
            hash = key.sha256(),
            owner = UUID.randomUUID(),
            fingerprint = fingerprint,
            takenAt = now,
            leaseUntil = now + properties.lease,
        )

        val holder = keys.take(mine, forgetBefore = now - properties.retention)

        return when {
            holder.owner == mine.owner -> IdempotentResult.Executed(run(mine, record, action))
            holder.fingerprint != fingerprint -> IdempotentResult.Mismatch
            holder.isCompleted() -> IdempotentResult.AlreadyDone(holder.outcome)
            else -> IdempotentResult.InFlight(Duration.between(now, holder.leaseUntil))
        }
    }

    private fun <T> run(mine: HeldKey, record: (T) -> ByteArray?, action: () -> T): T {
        val inTransaction = TransactionSynchronizationManager.isSynchronizationActive()
        if (inTransaction) TransactionSynchronizationManager.registerSynchronization(ReleaseOnRollback(mine))

        @Suppress("TooGenericExceptionCaught")
        try {
            val value = CurrentIdempotencyKey.with("${mine.scope}\n${mine.hash}".sha256(), action)
            keys.complete(mine, record(value), clock.instant())

            return value
        } catch (failure: Throwable) {
            if (!inTransaction) runCatching { keys.release(mine) }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
    }

    /**
     * The outcome commits with the writes of the action, in the transaction open around it: a rollback, or a crash
     * before the commit, leaves neither, and frees the key for the next try.
     */
    private inner class ReleaseOnRollback(private val key: HeldKey) : TransactionSynchronization {

        override fun afterCompletion(status: Int) {
            if (status != STATUS_COMMITTED) keys.release(key)
        }
    }
}
