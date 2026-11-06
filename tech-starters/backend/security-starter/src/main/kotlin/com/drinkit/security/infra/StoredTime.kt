package com.drinkit.security.infra

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

// PostgreSQL keeps microseconds and rounds the nanoseconds of an Instant to the nearest one, which could store a
// later instant than the real one and push an expiry past it. Truncating never stores a later instant.
internal fun Instant.toStoredTime(): OffsetDateTime = truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)
