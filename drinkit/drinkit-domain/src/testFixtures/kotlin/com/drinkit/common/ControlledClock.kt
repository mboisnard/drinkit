package com.drinkit.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ControlledClock(
    private val delegate: Clock = systemDefaultZone(),
    var fixedInstant: Instant? = null,
    var zoneId: ZoneId = delegate.zone,
) : Clock() {

    override fun instant(): Instant = fixedInstant ?: delegate.instant()

    override fun withZone(zone: ZoneId): Clock {
        if (this.zoneId == zone) {  // intentional NPE
            return this
        }

        return ControlledClock(delegate, fixedInstant, zone)
    }

    override fun getZone(): ZoneId = zoneId
}