package com.drinkit.messaging.infra.springevents

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.infra.Retry
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationListener
import org.springframework.context.PayloadApplicationEvent
import java.time.Duration

internal class PlatformEventHandlerListener(
    val handler: PlatformEventHandler,
    private val eventType: Class<*>,
    private val callHandler: (PlatformEvent<*>) -> Unit,
    private val firstAttempt: Retry,
    private val schedule: (Duration, () -> Unit) -> Unit = ::onVirtualThread,
) : ApplicationListener<PayloadApplicationEvent<*>> {

    private val logger = KotlinLogging.logger { }

    override fun onApplicationEvent(springEvent: PayloadApplicationEvent<*>) {
        val event = springEvent.payload
        if (event is PlatformEvent<*> && eventType.isInstance(event)) {
            deliver(event, firstAttempt, after = Duration.ofMillis(handler.delay))
        }
    }

    private fun deliver(event: PlatformEvent<*>, retry: Retry, after: Duration) {
        schedule(after) {
            runCatching { callHandler(event) }.onFailure { failure ->
                retryOrGiveUp(event, retry, failure.javaClass.simpleName)
            }
        }
    }

    private fun retryOrGiveUp(event: PlatformEvent<*>, retry: Retry, failure: String) {
        if (retry.attempt >= handler.maxAttempts) {
            logger.error {
                "Handler ${handler.name} gave up after ${retry.attempt} attempts, the last one failed with $failure"
            }
        } else {
            logger.warn {
                "Attempt ${retry.attempt} of handler ${handler.name} failed with $failure, " +
                    "next one in ${retry.waitBeforeNext}"
            }
            deliver(event, retry.next(), after = retry.waitBeforeNext)
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
