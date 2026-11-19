package com.drinkit.ai.infra

import com.drinkit.ai.AiModels
import com.drinkit.ai.CompatibleModels
import com.drinkit.ai.infra.observation.ChatContent
import com.drinkit.ai.infra.observation.GenAiConventions
import io.micrometer.observation.ObservationFilter
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

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

    @Bean
    @ConditionalOnMissingBean(name = ["aiGenAiConventions"])
    fun aiGenAiConventions(): ObservationFilter = GenAiConventions()

    @Bean
    @ConditionalOnBooleanProperty("starters.ai.observations.include-content")
    @ConditionalOnMissingBean(name = ["aiChatContent"])
    fun aiChatContent(jsonMapper: JsonMapper): ObservationFilter = ChatContent(jsonMapper)
}
