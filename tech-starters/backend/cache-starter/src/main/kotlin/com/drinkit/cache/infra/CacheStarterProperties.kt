package com.drinkit.cache.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_TTL_MINUTES = 10L
private const val DEFAULT_MAXIMUM_SIZE = 10_000L

@ConfigurationProperties("starters.cache")
internal data class CacheStarterProperties(
    val defaultTtl: Duration = Duration.ofMinutes(DEFAULT_TTL_MINUTES),
    val ttl: Map<String, Duration> = emptyMap(),
    val maximumSize: Long = DEFAULT_MAXIMUM_SIZE,
)
