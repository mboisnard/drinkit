package com.drinkit.sse

internal class SpyServerEventsTest : ServerEventsTestContract() {

    private val spy = SpyServerEvents()

    override fun fetchServerEvents(): ServerEvents = spy

    override fun eventNamesSentTo(userId: String) = spy.sentTo(userId).map { it.eventName }

    override fun eventNamesSentToSession(sessionId: String) = spy.sentToSession(sessionId).map { it.eventName }
}
