package com.drinkit.search.engine.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_TASK_TIMEOUT_SECONDS = 30L
private const val DEFAULT_BATCH_SIZE = 1_000
private const val DEFAULT_RESTART_AFTER_MINUTES = 30L

@ConfigurationProperties("starters.search-engine")
internal data class SearchEngineProperties(
    val meilisearch: Meilisearch = Meilisearch(),
    val taskTimeout: Duration = Duration.ofSeconds(DEFAULT_TASK_TIMEOUT_SECONDS),
    val rebuild: Rebuild = Rebuild(),
) {
    data class Meilisearch(val url: String = "http://localhost:7700", val apiKey: String? = null)

    data class Rebuild(
        val batchSize: Int = DEFAULT_BATCH_SIZE,
        val restartAfter: Duration = Duration.ofMinutes(DEFAULT_RESTART_AFTER_MINUTES),
    )
}
