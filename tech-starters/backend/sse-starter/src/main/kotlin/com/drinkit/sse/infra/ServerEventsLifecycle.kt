package com.drinkit.sse.infra

import org.springframework.context.SmartLifecycle
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

// At the default phase, it stops before the graceful shutdown, which would otherwise wait for every open stream
internal class ServerEventsLifecycle(private val streams: InMemoryStreams, private val heartbeat: Duration) :
    SmartLifecycle {

    private var heartbeats: ScheduledExecutorService? = null

    override fun start() {
        val period = heartbeat.toMillis()
        heartbeats = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("sse-heartbeat").factory())
            .apply { scheduleAtFixedRate(streams::ping, period, period, TimeUnit.MILLISECONDS) }
    }

    override fun stop() {
        heartbeats?.shutdownNow()
        heartbeats = null
        streams.closeAll()
    }

    override fun isRunning() = heartbeats != null
}
