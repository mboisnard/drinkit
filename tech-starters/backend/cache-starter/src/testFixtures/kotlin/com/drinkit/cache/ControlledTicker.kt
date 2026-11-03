package com.drinkit.cache

import com.github.benmanes.caffeine.cache.Ticker
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/** The time of the caches in a test: declared as a bean, it expires an entry once [advance] passes its time to live. */
class ControlledTicker : Ticker {

    private val nanos = AtomicLong()

    override fun read(): Long = nanos.get()

    fun advance(duration: Duration) {
        nanos.addAndGet(duration.toNanos())
    }
}
