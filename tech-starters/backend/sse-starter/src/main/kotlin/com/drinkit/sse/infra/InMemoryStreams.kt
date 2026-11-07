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

    fun open(recipient: Recipient, stream: SseBuilder) {
        stream.onTimeout(stream::complete)
        stream.onError { error -> forget(recipient, stream, error) }
        stream.onComplete { forget(recipient, stream) }

        val streamsOfRecipient = add(recipient, stream)
        if (streamsOfRecipient.size > maxStreamsPerRecipient) {
            streamsOfRecipient.removeAt(0).complete()
        }

        write(recipient, stream) { comment("").send() }
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

    fun completeAll() {
        streams.values.flatten().forEach(SseBuilder::complete)
    }

    // compute, never get then add: a stream must not join a list that forget is removing
    private fun add(recipient: Recipient, stream: SseBuilder) = checkNotNull(
        streams.compute(recipient) { _, open -> (open ?: CopyOnWriteArrayList()).apply { add(stream) } },
    )

    // A stream writes its fields one by one: the heartbeat and an event must not interleave theirs
    private fun write(recipient: Recipient, stream: SseBuilder, fields: SseBuilder.() -> Unit) {
        runCatching { synchronized(stream) { stream.fields() } }
            .onFailure { error ->
                forget(recipient, stream, error)
                stream.complete()
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
