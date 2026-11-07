package com.drinkit.sse.infra

import org.springframework.web.servlet.function.ServerResponse.SseBuilder
import java.time.Duration
import java.util.function.Consumer

/**
 * The stream of one browser tab, as Spring writes it: fields pile up until a send writes them with a blank line.
 * It throws [failureAfterOpening] on every send after the opening comment.
 */
internal class RecordingSseBuilder(
    private val failureAfterOpening: Exception? = null,
    private val completeFailure: Exception? = null,
) : SseBuilder {

    val sent = mutableListOf<String>()
    var completed = false
        private set

    private val pending = StringBuilder()
    private var onTimeout: Runnable? = null
    private var onError: Consumer<Throwable>? = null
    private var onComplete: Runnable? = null

    fun timeOut() = checkNotNull(onTimeout).run()

    fun failConnection(error: Throwable) = checkNotNull(onError).accept(error)

    override fun send() {
        if (sent.isNotEmpty()) failureAfterOpening?.let { throw it }
        sent += pending.append('\n').toString()
        pending.clear()
    }

    override fun send(`object`: Any) = data(`object`)

    override fun data(`object`: Any) {
        pending.append("data:").append(`object`).append('\n')
        send()
    }

    override fun event(eventName: String) = apply { pending.append("event:").append(eventName).append('\n') }

    override fun comment(comment: String) = apply { pending.append(':').append(comment).append('\n') }

    override fun id(id: String) = apply { pending.append("id:").append(id).append('\n') }

    override fun retry(duration: Duration) = apply { pending.append("retry:").append(duration.toMillis()).append('\n') }

    override fun complete() {
        completeFailure?.let { throw it }
        if (completed) return
        completed = true
        onComplete?.run()
    }

    override fun error(t: Throwable) = complete()

    override fun onTimeout(onTimeout: Runnable) = apply { this.onTimeout = onTimeout }

    override fun onError(onError: Consumer<Throwable>) = apply { this.onError = onError }

    override fun onComplete(onCompletion: Runnable) = apply { this.onComplete = onCompletion }
}
