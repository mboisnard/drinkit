package com.drinkit.sse

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface ServerEvents {

    fun send(userId: String, eventName: String, payload: Any)

    fun sendToSession(sessionId: String, eventName: String, payload: Any)
}
