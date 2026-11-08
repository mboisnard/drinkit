package com.drinkit.idempotency

import com.drinkit.idempotency.infra.HeldKey
import com.drinkit.idempotency.infra.IdempotencyKeys
import com.drinkit.idempotency.infra.IdempotencyProperties
import com.drinkit.idempotency.infra.KeyedIdempotency
import java.time.Clock
import java.time.Instant

/** The starter's [Idempotency], its keys kept in memory. */
fun inMemoryIdempotency(clock: Clock = Clock.systemUTC()): Idempotency =
    KeyedIdempotency(InMemoryIdempotencyKeys(), clock, IdempotencyProperties())

internal class InMemoryIdempotencyKeys : IdempotencyKeys {

    private val keys = mutableMapOf<Pair<String, String>, HeldKey>()

    @Synchronized
    override fun take(key: HeldKey, forgetBefore: Instant): HeldKey {
        keys.values.removeIf { it.takenAt < forgetBefore }

        val holder = keys[key.scope to key.hash]?.takeUnless { it.canBeTakenBy(key, forgetBefore) } ?: key
        keys[key.scope to key.hash] = holder

        return holder
    }

    @Synchronized
    override fun complete(key: HeldKey, outcome: ByteArray?, at: Instant) {
        val holder = checkNotNull(keys[key.scope to key.hash]?.takeIf { it.owner == key.owner }) {
            "Another run took over the key once the lease of this one ended"
        }

        keys[key.scope to key.hash] = holder.completed(outcome, at)
    }

    @Synchronized
    override fun release(key: HeldKey) {
        keys.computeIfPresent(key.scope to key.hash) { _, holder -> holder.takeIf { it.owner != key.owner } }
    }
}
