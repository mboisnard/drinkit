package com.drinkit.postgresql.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_SLOW_QUERY_THRESHOLD_MILLIS = 500L

@ConfigurationProperties("starters.postgresql")
internal data class PostgresqlProperties(
    val slowQueryThreshold: Duration = Duration.ofMillis(DEFAULT_SLOW_QUERY_THRESHOLD_MILLIS),
)
