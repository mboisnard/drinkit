package com.drinkit.webclient.infra

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.webclient", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(WebClientProperties::class)
internal class WebClientAutoConfiguration {

    // By name: the application declares group configurers of its own, which must not switch the retries off
    @Bean
    @ConditionalOnMissingBean(name = ["retryingHttpServiceGroupConfigurer"])
    fun retryingHttpServiceGroupConfigurer(properties: WebClientProperties) =
        RestClientHttpServiceGroupConfigurer { groups ->
            groups.forEachClient { group, clientBuilder ->
                val honoursIdempotencyKey = group.name() in properties.retry.idempotentGroups
                clientBuilder.requestInterceptor(RequestRetryInterceptor(properties.retry, honoursIdempotencyKey))
            }
        }
}
