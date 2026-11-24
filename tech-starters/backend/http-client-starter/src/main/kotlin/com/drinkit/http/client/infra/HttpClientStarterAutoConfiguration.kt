package com.drinkit.http.client.infra

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer

@AutoConfiguration
@ConditionalOnBooleanProperty("starters.http-client.enabled", matchIfMissing = true)
@EnableConfigurationProperties(HttpClientStarterProperties::class)
internal class HttpClientStarterAutoConfiguration {

    // By name: the application declares group configurers of its own, which must not switch the retries off
    @Bean
    @ConditionalOnMissingBean(name = ["retryingHttpServiceGroupConfigurer"])
    fun retryingHttpServiceGroupConfigurer(properties: HttpClientStarterProperties) =
        RestClientHttpServiceGroupConfigurer { groups ->
            groups.forEachClient { group, clientBuilder ->
                val honoursIdempotencyKey = group.name() in properties.retry.idempotentGroups
                clientBuilder.requestInterceptor(RequestRetryInterceptor(properties.retry, honoursIdempotencyKey))
            }
        }
}
