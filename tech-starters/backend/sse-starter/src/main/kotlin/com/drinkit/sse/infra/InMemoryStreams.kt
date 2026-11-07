package com.drinkit.sse.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.servlet.function.ServerResponse.SseBuilder
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** The open streams of each recipient, one per browser tab. */
internal class InMemoryStreams(private val maxStreamsPerRecipient: Int) {

    private val logger = KotlinLogging.logger { }

    // A send iterates over a copy of the list, so that tabs can open and close meanwhile
    private val streams = ConcurrentHashMap<Recipient, CopyOnWriteArrayList<SseBuilder>>()

    // Once the application stops, a stream opened in the meantime would hold the graceful shutdown
    @Volatile
    private var closed = false

    fun open(recipient: Recipient, stream: SseBuilder) {
        stream.onTimeout(stream::complete)
        stream.onError { error -> forget(recipient, stream, error) }
        stream.onComplete { forget(recipient, stream) }

        // In one step with the add, as the tabs of a recipient may open at the same moment
        var added = false
        var oldest: SseBuilder? = null
        streams.compute(recipient) { _, open ->
            (open ?: CopyOnWriteArrayList()).apply {
                if (closed) return@apply
                if (size >= maxStreamsPerRecipient) oldest = removeAt(0)
                added = add(stream)
            }.ifEmpty { null }
        }

        // A tab only closes its stream on the server at the next write, so the oldest one is likely gone already
        oldest?.let { runCatching { it.complete() } }

        // Completed at once while the application stops, for the browser to open it again on another instance
        if (added) write(recipient, stream) { comment("").send() } else runCatching { stream.complete() }
    }

    fun send(recipient: Recipient, eventName: String, json: String) {
        streams[recipient]?.forEach { stream ->
            write(recipient, stream) { event(eventName).data(json) }
        }
    }

    fun ping() {
        streams.forEach { (recipient, streamsOfRecipient) ->
            streamsOfRecipient.forEach { stream -> write(recipient, stream) { comment("").send() } }
        }
    }

    fun closeAll() {
        closed = true
        streams.values.flatten().forEach { stream -> runCatching { stream.complete() } }
    }

    // A stream writes its fields one by one: the heartbeat and an event must not interleave theirs
    private fun write(recipient: Recipient, stream: SseBuilder, fields: SseBuilder.() -> Unit) {
        runCatching { synchronized(stream) { stream.fields() } }
            .onFailure { error ->
                forget(recipient, stream, error)
                // A response the container already completed throws on complete too
                runCatching { stream.complete() }
            }
    }

    private fun forget(recipient: Recipient, stream: SseBuilder, error: Throwable? = null) {
        when (error) {
            null -> Unit
            is IOException -> logger.debug(error) { "A browser tab went away" }
            else -> logger.warn(error) { "A server events stream failed" }
        }

        streams.computeIfPresent(recipient) { _, open -> open.apply { remove(stream) }.ifEmpty { null } }
    }
}
