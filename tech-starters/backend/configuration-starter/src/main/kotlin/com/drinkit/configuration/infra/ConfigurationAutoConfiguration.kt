package com.drinkit.configuration.infra

import com.drinkit.cache.CacheTtl
import com.drinkit.configuration.Configurations
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Duration

private const val CACHE_TTL_MINUTES = 10L

@AutoConfiguration(after = [JooqAutoConfiguration::class])
@ConditionalOnProperty(prefix = "starters.configuration", name = ["enabled"], matchIfMissing = true)
internal class ConfigurationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun configurations(dsl: DSLContext, jsonMapper: JsonMapper, clock: Clock): Configurations =
        JooqConfigurations(dsl, jsonMapper, clock)

    // A value changed in the table, or by another instance, is read again once its entry expires
    @Bean
    @ConditionalOnMissingBean(name = ["configurationsCacheTtl"])
    fun configurationsCacheTtl(): CacheTtl = CacheTtl(CONFIGURATIONS_CACHE, Duration.ofMinutes(CACHE_TTL_MINUTES))
}
