package com.drinkit.sse.infra

import com.drinkit.cache.CacheTtl
import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.sse.ServerEvents
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCache
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

internal const val SERVER_EVENTS_PATH = "/sse/event-stream"

@AutoConfiguration(after = [JacksonAutoConfiguration::class, CacheAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.sse", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SseProperties::class)
internal class SseAutoConfiguration {

    // A tab hidden longer reads its data again when it is shown, whether or not it missed an event
    @Bean
    @ConditionalOnMissingBean(name = ["lastEventIdsCacheTtl"])
    fun lastEventIdsCacheTtl() = CacheTtl(LAST_EVENT_IDS_CACHE, Duration.ofHours(1))

    @Bean
    @ConditionalOnMissingBean
    fun lastEventIds(cacheManager: ObjectProvider<CacheManager>): LastEventIds {
        // Without a cache, a stream opens with a new id each time, so a tab reads its data again at each opening
        val cache = cacheManager.ifAvailable?.getCache(LAST_EVENT_IDS_CACHE) ?: NoOpCache(LAST_EVENT_IDS_CACHE)

        return LastEventIds(cache)
    }

    @Bean
    @ConditionalOnMissingBean
    fun serverEventStreams(properties: SseProperties, lastEventIds: LastEventIds) =
        InMemoryStreams(properties.maxStreams, lastEventIds)

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
    fun serverEventsSubscription(streams: InMemoryStreams, properties: SseProperties) =
        ServerEventsSubscription(streams, properties.timeout)

    @Bean
    @ConditionalOnMissingBean(name = ["serverEventsEndpoint"])
    fun serverEventsEndpoint(subscription: ServerEventsSubscription): RouterFunction<ServerResponse> =
        RouterFunctions.route().GET(SERVER_EVENTS_PATH, subscription::subscribe).build()

    @Bean
    @ConditionalOnMissingBean
    fun serverEventsDelivery(streams: InMemoryStreams) = ServerEventsDelivery(streams)

    @Bean
    @ConditionalOnMissingBean
    fun serverEventsLifecycle(streams: InMemoryStreams, properties: SseProperties) =
        ServerEventsLifecycle(streams, properties.heartbeat)
}
