package com.drinkit.sse

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.events.SendSseEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Duration

private val EMITTER_TIMEOUT = Duration.ofMinutes(3)

@TechStarterTool
@RestController("/sse")
internal class SseApi(
    private val emitters: InMemoryEmittersRepository,
) {
    private val logger = KotlinLogging.logger { }

    @GetMapping("/event-stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribeToEventStream(request: HttpServletRequest, @RequestParam eventName: String): SseEmitter {
        val sessionId = request.session.id
        val emitter = createEmitterFor(sessionId)

        emitters.register(sessionId, eventName, emitter)

        logger.debug { "Registered emitter for session: $sessionId" }

        return emitter
    }

    @PlatformEventHandler(name = "send.sse.event.to.connected.event.stream.queue", oneQueuePerInstance = true)
    fun sendMessageToEventStream(platformEvent: SendSseEvent) {

        val emitters = emitters.findAllBy(platformEvent.sessionId)
            ?: return logger.debug { "No emitters found on this instance for session: ${platformEvent.sessionId}" }

        val sseEvent = SseEmitter.event()
            .id(platformEvent.sessionId)
            .name(platformEvent.eventName)
            .data(platformEvent.payload)

        emitters.filter { it.eventName == platformEvent.eventName }.forEach {
            try {
                it.emitter.send(sseEvent)
            } catch (ex: Exception) {
                logger.debug(ex) { "Completing emitter when sending event" }
                it.emitter.completeWithError(ex)
            }
        }

        logger.debug { "Sending message to event stream" }
    }

    private fun createEmitterFor(sessionId: String): SseEmitter {
        val emitter = SseEmitter(EMITTER_TIMEOUT.toMillis())

        emitter.onCompletion {
            logger.debug { "Emitter completion" }
            emitters.unregister(sessionId, emitter)
        }

        emitter.onError {
            logger.warn(it) { "Emitter error" }
            emitters.unregister(sessionId, emitter)
        }

        emitter.onTimeout {
            logger.info { "Emitter timeout for session: $sessionId" }
            emitters.unregister(sessionId, emitter)
        }

        return emitter
    }
}