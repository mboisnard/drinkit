package com.drinkit.sse.infra

import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.sse.ServerEvents
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

internal const val SERVER_EVENTS_PATH = "/sse/event-stream"

@AutoConfiguration(after = [JacksonAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.sse", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SseProperties::class)
internal class SseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun serverEventStreams(properties: SseProperties) = InMemoryStreams(properties.maxStreams)

    @Bean
    @ConditionalOnMissingBean
    fun serverEvents(publisher: ObjectProvider<PlatformEventPublisher>, jsonMapper: JsonMapper): ServerEvents {
        val platformEventPublisher = checkNotNull(publisher.ifAvailable) {
            "The SSE starter delivers through the messaging starter: keep starters.messaging.enabled"
        }

        return QueuedServerEvents(platformEventPublisher, jsonMapper)
    }

    @Bean
    @ConditionalOnMissingBean
    fun serverEventsApi(streams: InMemoryStreams, properties: SseProperties) =
        ServerEventsApi(streams, properties.timeout)

    @Bean
    @ConditionalOnMissingBean(name = ["serverEventsEndpoint"])
    fun serverEventsEndpoint(api: ServerEventsApi): RouterFunction<ServerResponse> =
        RouterFunctions.route().GET(SERVER_EVENTS_PATH, api::subscribe).build()

    @Bean
    @ConditionalOnMissingBean
    fun serverEventsLifecycle(streams: InMemoryStreams, properties: SseProperties) =
        ServerEventsLifecycle(streams, properties.heartbeat)
}
