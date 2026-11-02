package com.drinkit.postgresql.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.ExecuteContext
import org.jooq.ExecuteListener
import java.time.Duration

internal class SlowQueryListener(val threshold: Duration, private val nanoTime: () -> Long = System::nanoTime) :
    ExecuteListener {

    private val logger = KotlinLogging.logger { }

    override fun executeStart(ctx: ExecuteContext) {
        ctx.data(START, nanoTime())
    }

    override fun executeEnd(ctx: ExecuteContext) {
        val start = ctx.data(START) as? Long ?: return
        val duration = Duration.ofNanos(nanoTime() - start)
        if (duration > threshold) {
            // The SQL holds a ? for each bind value, never the value: no personal data reaches the logs
            logger.warn {
                "A query took ${duration.toMillis()} ms, over the ${threshold.toMillis()} ms threshold: ${ctx.sql()}"
            }
        }
    }

    private companion object {
        const val START = "com.drinkit.postgresql.slow-query.start"
    }
}
