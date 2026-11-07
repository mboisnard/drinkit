package com.drinkit.sse.infra

import com.drinkit.security.EndpointRules
import com.drinkit.security.endpointRules
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod

// An auto-configuration of its own, since starters.no-stereotype refuses a nested @Configuration to guard the bean
@AutoConfiguration(after = [SseAutoConfiguration::class])
@ConditionalOnClass(EndpointRules::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.sse", name = ["enabled"], matchIfMissing = true)
internal class SseSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["serverEventsAccessRules"])
    fun serverEventsAccessRules(): EndpointRules = endpointRules {
        // Out of /api/**, so that a broad rule of the application never decides for the stream before this one
        authorize(HttpMethod.GET, SERVER_EVENTS_PATH, permitAll)
    }
}
