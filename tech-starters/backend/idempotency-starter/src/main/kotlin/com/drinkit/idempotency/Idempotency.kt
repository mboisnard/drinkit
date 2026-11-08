package com.drinkit.idempotency

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.Duration

/**
 * Runs an action once per key, however many times a client retries it or a broker delivers it again.
 *
 * A run takes the key before the action, in a transaction of its own that every instance sees, and records the
 * outcome once the action succeeds. A transaction open around [once] holds that outcome with the writes of the
 * action: a rollback, or a crash before the commit, leaves neither. A failure frees the key for the next try, once
 * that transaction rolls back if there is one, and a crashed run leaves it to the next one once its lease ends.
 */
@TechStarterTool
interface Idempotency {

    /**
     * Runs [action] unless an earlier run under the same [scope] and [key] finished, or still runs. [fingerprint]
     * tells a replay from another payload sent under the same key, [record] what a replay gets back.
     */
    fun <T> once(
        scope: String,
        key: String,
        fingerprint: String = "",
        record: (T) -> ByteArray? = { null },
        action: () -> T,
    ): IdempotentResult<T>
}

@TechStarterTool
sealed interface IdempotentResult<out T> {

    data class Executed<T>(val value: T) : IdempotentResult<T>

    /** An earlier run did it, and [recorded] what a replay gets back. */
    class AlreadyDone(val recorded: ByteArray?) : IdempotentResult<Nothing>

    /** Another run holds the key, and frees it at the latest in [retryAfter]. */
    data class InFlight(val retryAfter: Duration) : IdempotentResult<Nothing>

    /** The key was taken for another payload. */
    data object Mismatch : IdempotentResult<Nothing>
}
