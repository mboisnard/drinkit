package com.drinkit.test

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class ControlledClock(
    private val delegate: Clock = systemDefaultZone(),
    private var fixedInstant: Instant? = null,
    var zoneId: ZoneId = delegate.zone,
) : Clock() {

    override fun getZone(): ZoneId = zoneId
    override fun instant(): Instant = fixedInstant ?: delegate.instant()
    override fun withZone(zone: ZoneId): Clock = ControlledClock(delegate, fixedInstant, zone)

    fun fix() = apply { fixedInstant = delegate.instant() }

    fun fix(instant: Instant) = apply { fixedInstant = instant }

    fun add(duration: Duration) = apply {
        require(fixedInstant != null) { "Clock must be fixed with fix() before advancing time." }
        fixedInstant = fixedInstant?.plus(duration)
    }

    fun reset() = apply { fixedInstant = null }
}
