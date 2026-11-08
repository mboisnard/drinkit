package com.drinkit.messaging.infra.springevents

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.IdempotentResult
import com.drinkit.kotlin.sha256
import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.infra.Retry
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationListener
import org.springframework.context.PayloadApplicationEvent
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import java.time.Duration

/**
 * Calls one handler with the events of its type, on a virtual thread, after its delay, and again on failure up to
 * its attempts. An idempotent handler runs once per key of its event, however many times the event is delivered.
 */
internal class PlatformEventHandlerListener(
    val handler: PlatformEventHandler,
    private val eventType: Class<*>,
    private val callHandler: (PlatformEvent<*>) -> Unit,
    private val firstAttempt: Retry,
    // Null for a handler that is not idempotent
    private val idempotency: Idempotency? = null,
    private val schedule: (Duration, () -> Unit) -> Unit = ::onVirtualThread,
) : ApplicationListener<PayloadApplicationEvent<*>> {

    private val logger = KotlinLogging.logger { }

    private val keyExpression = handler.idempotencyKey.takeIf { it.isNotEmpty() }
        ?.let { SpelExpressionParser().parseExpression(it) }

    override fun onApplicationEvent(springEvent: PayloadApplicationEvent<*>) {
        val delivery = springEvent.payload
        if (delivery is Delivery && eventType.isInstance(delivery.event)) {
            deliver(delivery, firstAttempt, after = Duration.ofMillis(handler.delay))
        }
    }

    private fun deliver(delivery: Delivery, retry: Retry, after: Duration) {
        schedule(after) {
            runCatching { handle(delivery) }
                // Not an attempt: it comes back once the other run ends, or once its lease does if it crashed
                .onSuccess { stillRunningFor -> stillRunningFor?.let { deliver(delivery, retry, after = it) } }
                .onFailure { failure ->
                    logger.debug(failure) { "Attempt ${retry.attempt} of handler ${handler.name} failed" }
                    retryOrGiveUp(delivery, retry, failure.javaClass.simpleName)
                }
        }
    }

    /** Null once handled, else how long another run still holds its key. */
    private fun handle(delivery: Delivery): Duration? {
        // The events the handler publishes derive their key from its own, so that two handlers never share one
        val key = "${handler.name}\n${delivery.idempotencyKey}".sha256()

        return CurrentIdempotencyKey.with(key) {
            if (idempotency == null) {
                callHandler(delivery.event)
                null
            } else {
                val result = idempotency.once(handler.name, keyOf(delivery)) { callHandler(delivery.event) }
                (result as? IdempotentResult.InFlight)?.retryAfter
            }
        }
    }

    private fun keyOf(delivery: Delivery): String {
        val expression = keyExpression ?: return delivery.idempotencyKey

        val context = SimpleEvaluationContext.forReadOnlyDataBinding().build()
        context.setVariable("event", delivery.event)

        val key = checkNotNull(expression.getValue(context)) {
            "Handler ${handler.name}: ${handler.idempotencyKey} is null"
        }

        return key.toString()
    }

    private fun retryOrGiveUp(delivery: Delivery, retry: Retry, failure: String) {
        if (retry.attempt >= handler.maxAttempts) {
            logger.error {
                "Handler ${handler.name} gave up after ${retry.attempt} attempts, the last one failed with $failure"
            }
        } else {
            logger.warn {
                "Attempt ${retry.attempt} of handler ${handler.name} failed with $failure, " +
                    "next one in ${retry.waitBeforeNext}"
            }
            deliver(delivery, retry.next(), after = retry.waitBeforeNext)
        }
    }
}

// A factory, unlike a Thread.Builder, can be shared by the threads that publish at once
private val virtualThreads = Thread.ofVirtual().name("platform-event-", 0).factory()

private fun onVirtualThread(delay: Duration, task: () -> Unit) {
    virtualThreads.newThread {
        Thread.sleep(delay)
        task()
    }.start()
}
