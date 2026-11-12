package com.drinkit.ai.infra

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/** Counts the input and output tokens of each model as `gen_ai.client.token.usage`. */
@AutoConfiguration(
    afterName = ["org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration"],
)
@ConditionalOnBean(MeterRegistry::class)
internal class AiMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun chatModelMeterObservationHandler(meterRegistry: MeterRegistry) = ChatModelMeterObservationHandler(meterRegistry)
}
