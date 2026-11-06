package com.drinkit.config

import com.drinkit.security.endpointRules
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class SecurityConfiguration {

    @Bean
    fun drinkitEndpoints() = endpointRules {
        authorize("/api/registration/**", authenticated)
        authorize("/api/user/me", authenticated)
        authorize("/api/**", hasRole("USER"))
        authorize("/actuator/**", hasRole("ADMIN"))
        authorize("/openapi/**", hasRole("ADMIN"))
    }
}
