package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import com.drinkit.ai.CompatibleModels
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(AiProperties::class)
internal class AiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun aiModels(
        properties: AiProperties,
        observationRegistry: ObjectProvider<ObservationRegistry>,
        customizers: ObjectProvider<ChatClientBuilderCustomizer>,
    ): AiModels = DefaultAiModels(
        properties,
        observationRegistry.getIfUnique { ObservationRegistry.NOOP },
        customizers.orderedStream().toList(),
    )

    @Bean
    @ConditionalOnMissingBean
    fun compatibleModels(aiModels: AiModels): CompatibleModels = CompatibleModels(aiModels)
}
