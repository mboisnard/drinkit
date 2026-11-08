package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.infra.flipt.fliptHealthIndicator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [FeatureManagementAutoConfiguration::class])
@ConditionalOnClass(HealthIndicator::class)
@ConditionalOnBean(FeatureManagementProperties::class)
@ConditionalOnProperty(prefix = "starters.feature-management", name = ["enabled"], matchIfMissing = true)
internal class FeatureManagementHealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["fliptHealthIndicator"])
    fun fliptHealthIndicator(properties: FeatureManagementProperties): HealthIndicator =
        fliptHealthIndicator(properties.flipt)
}
