package com.drinkit.idempotency.infra

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.infra.jooq.IdempotencyKeys
import com.drinkit.idempotency.infra.jooq.JooqIdempotencyKeys
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import java.time.Clock

@AutoConfiguration(after = [JooqAutoConfiguration::class])
@ConditionalOnBean(DSLContext::class)
@ConditionalOnProperty(prefix = "starters.idempotency", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(IdempotencyProperties::class)
internal class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun idempotencyKeys(dsl: DSLContext): IdempotencyKeys = JooqIdempotencyKeys(dsl)

    @Bean
    @ConditionalOnMissingBean
    fun idempotency(keys: IdempotencyKeys, clock: Clock, properties: IdempotencyProperties): Idempotency =
        KeyedIdempotency(keys, clock, properties)
}
