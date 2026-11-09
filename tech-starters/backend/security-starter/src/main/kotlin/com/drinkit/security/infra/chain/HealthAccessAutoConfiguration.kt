package com.drinkit.security.infra.chain

import com.drinkit.security.EndpointRules
import com.drinkit.security.endpointRules
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@AutoConfiguration
@ConditionalOnClass(WebEndpointAutoConfiguration::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
internal class HealthAccessAutoConfiguration {

    // First, so that a rule of the application keeping the actuator to admins never shuts out the probes
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = ["healthAccessRules"])
    fun healthAccessRules(): EndpointRules = endpointRules {
        authorize(EndpointRequest.to("health"), permitAll)
    }
}
