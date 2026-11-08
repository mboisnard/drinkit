package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.infra.openfeature.EXPOSURES_PATH
import com.drinkit.featuremanagement.infra.openfeature.OFREP_BULK_PATH
import com.drinkit.security.EndpointRules
import com.drinkit.security.endpointRules
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod

// Of its own, so that the coverage of the application's access rules finds it without the other beans
@AutoConfiguration
@ConditionalOnClass(EndpointRules::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
internal class FeatureManagementSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["featureManagementAccessRules"])
    fun featureManagementAccessRules(): EndpointRules = endpointRules {
        authorize(HttpMethod.POST, OFREP_BULK_PATH, permitAll)
        authorize(HttpMethod.POST, EXPOSURES_PATH, permitAll)
    }
}
