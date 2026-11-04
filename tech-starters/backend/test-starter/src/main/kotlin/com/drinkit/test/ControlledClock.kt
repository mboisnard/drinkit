package com.drinkit.test

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@TechStarterTool
class ControlledClock(
    private val delegate: Clock = systemDefaultZone(),
    // As a bean of a Spring test, the test thread moves it while the threads of the application read it
    @Volatile private var fixedInstant: Instant? = null,
    var zoneId: ZoneId = delegate.zone,
) : Clock() {

    override fun getZone(): ZoneId = zoneId
    override fun instant(): Instant = fixedInstant ?: delegate.instant()
    override fun withZone(zone: ZoneId): Clock = ControlledClock(delegate = this, zoneId = zone)

    fun fix() = apply { fixedInstant = delegate.instant() }

    fun fix(instant: Instant) = apply { fixedInstant = instant }

    fun add(duration: Duration) = apply {
        val instant = requireNotNull(fixedInstant) { "Clock must be fixed with fix() before advancing time." }
        fixedInstant = instant + duration
    }

    fun reset() = apply { fixedInstant = null }
}
