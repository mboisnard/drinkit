package com.drinkit.sse

import org.springframework.stereotype.Repository
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal data class Emitter(
    val eventName: String,
    val emitter: SseEmitter,
)

/**
 * Thread-safe repository for managing SSE emitters.
 *
 * Thread safety is critical here because:
 * - HTTP requests (subscribe) run on servlet threads and call [register]
 * - Message handlers (event dispatch) run on messaging thread pools and iterate emitters
 * - Callback handlers (timeout, completion, error) run on various threads and call [unregister]
 *
 * Uses [ConcurrentHashMap] for thread-safe map operations and [CopyOnWriteArrayList] for
 * safe concurrent iterations (iteration snapshots prevent ConcurrentModificationException
 * when adding/removing emitters while dispatching events).
 */
@Repository
internal class InMemoryEmittersRepository {

    // CopyOnWriteArrayList = Another way to make it thread-safe, like synchronizedList(mutableListOf())
    private val emitters = ConcurrentHashMap<String, CopyOnWriteArrayList<Emitter>>()

    fun findAllBy(sessionId: String) = emitters[sessionId]

    fun register(sessionId: String, eventName: String, emitter: SseEmitter) {
        val emittersForSessionId = emitters.computeIfAbsent(sessionId) {
            CopyOnWriteArrayList()
        }
        emittersForSessionId.add(Emitter(eventName, emitter))
    }

    fun unregister(sessionId: String, emitter: SseEmitter) {
        // removeIf is thread-safe on CopyOnWriteArrayList
        emitters[sessionId]?.removeIf { it.emitter == emitter }

        emitters.computeIfPresent(sessionId) { _, list ->
            if (list.isEmpty()) null else list
        }
    }
}
