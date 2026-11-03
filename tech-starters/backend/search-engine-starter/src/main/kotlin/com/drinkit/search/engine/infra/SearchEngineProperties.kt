package com.drinkit.search.engine.infra

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("starters.search-engine")
internal data class SearchEngineProperties(val meilisearch: Meilisearch = Meilisearch()) {
    data class Meilisearch(val url: String = "http://localhost:7700", val apiKey: String? = null)
}
