package com.drinkit.cache.infra

import com.drinkit.cache.CacheTtl
import com.github.benmanes.caffeine.cache.Ticker
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean

// Before Spring Boot's: its cache manager only exists once @EnableCaching is there
@AutoConfiguration(before = [CacheAutoConfiguration::class])
@ConditionalOnProperty(prefix = "starters.cache", name = ["enabled"], matchIfMissing = true)
@EnableCaching
@EnableConfigurationProperties(CacheStarterProperties::class)
internal class CacheStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["cacheStarterCaffeineCustomizer"])
    fun cacheStarterCaffeineCustomizer(
        properties: CacheStarterProperties,
        declared: ObjectProvider<CacheTtl>,
        ticker: ObjectProvider<Ticker>,
    ): CacheManagerCustomizer<CaffeineCacheManager> = CaffeineTtls(
        properties = properties,
        declared = declared.orderedStream().toList(),
        // A test declares a Ticker bean to move time forward
        ticker = ticker.getIfAvailable { Ticker.systemTicker() },
    )

    companion object {

        // Static, as a BeanPostProcessor must be, so that it sees the cache manager when Spring creates it
        @Bean
        @JvmStatic
        @ConditionalOnMissingBean(name = ["transactionAwareCaches"])
        fun transactionAwareCaches(): BeanPostProcessor = TransactionAwareCaches()
    }
}
