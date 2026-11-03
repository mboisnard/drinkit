package com.drinkit.cache.infra

import com.drinkit.cache.CacheTtl
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer
import org.springframework.cache.caffeine.CaffeineCacheManager
import java.time.Duration

internal class CaffeineTtls(
    private val properties: CacheStarterProperties,
    private val declared: List<CacheTtl>,
    private val ticker: Ticker,
) : CacheManagerCustomizer<CaffeineCacheManager> {

    override fun customize(cacheManager: CaffeineCacheManager) {
        cacheManager.setCaffeine(caffeine(properties.defaultTtl))

        val ttlByCache = declared.associate { it.cacheName to it.ttl } + properties.ttl
        ttlByCache.forEach { (cacheName, ttl) -> cacheManager.registerCustomCache(cacheName, caffeine(ttl).build()) }
    }

    private fun caffeine(ttl: Duration): Caffeine<Any, Any> = Caffeine.newBuilder()
        .expireAfterWrite(ttl)
        .maximumSize(properties.maximumSize)
        .recordStats()
        .ticker(ticker)
}
