package com.drinkit.idempotency

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.drinkit.kotlin.sha256
import org.slf4j.MDC

private const val LOG_KEY = "idempotency.key"

/**
 * The key of the request or the event being handled on this thread. The events it publishes and the providers it
 * calls derive their own key from it, so that a replay sends them under the same keys and they recognize it.
 */
@TechStarterTool
object CurrentIdempotencyKey {

    private class Current(val key: String) {
        val published = mutableMapOf<String, Int>()
    }

    private val current = ThreadLocal<Current>()

    fun get(): String? = current.get()?.key

    /** The key of the next event of [type]: a replay publishes its events in the same order, so under the same keys. */
    fun derive(type: String): String? = current.get()?.let {
        val rank = it.published.merge(type, 1, Int::plus)

        "${it.key}\n$type\n$rank".sha256()
    }

    fun <T> with(key: String, action: () -> T): T {
        val previous = current.get()
        current.set(Current(key))
        MDC.put(LOG_KEY, key)

        try {
            return action()
        } finally {
            if (previous == null) current.remove() else current.set(previous)
            if (previous == null) MDC.remove(LOG_KEY) else MDC.put(LOG_KEY, previous.key)
        }
    }
}
