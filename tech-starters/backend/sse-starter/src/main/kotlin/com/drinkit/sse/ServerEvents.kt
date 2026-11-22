package com.drinkit.sse

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface ServerEvents {

    fun send(userId: String, eventName: String, payload: Any)

    fun sendToSession(sessionId: String, eventName: String, payload: Any)

    companion object {
        /** The event that opens every stream, with the id of the last event sent to its recipient. */
        internal const val OPENING_EVENT = "init"

        // Checked where the caller sees it, by every implementation: a stream would only fail on it and close
        fun requireEventName(eventName: String) {
            require(eventName.isNotBlank() && eventName.lines().size == 1) { "An event name is one line, not blank" }
            require(eventName != OPENING_EVENT) { "$OPENING_EVENT opens every stream: name the event otherwise" }
        }
    }
}
