package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.Experiments
import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.featuremanagement.SubjectAttributes
import com.drinkit.featuremanagement.infra.openfeature.EXPOSURES_PATH
import com.drinkit.featuremanagement.infra.openfeature.OFREP_BULK_PATH
import com.drinkit.featuremanagement.infra.openfeature.OfrepApi
import com.drinkit.security.CurrentUser
import com.drinkit.security.CurrentVisitor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

@AutoConfiguration(after = [FeatureManagementAutoConfiguration::class, JacksonAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
internal class FeatureManagementWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun subjectAttributes() = SubjectAttributes { emptyMap() }

    @Bean
    @ConditionalOnMissingBean
    fun requestSubject(currentUser: CurrentUser, currentVisitor: CurrentVisitor, attributes: SubjectAttributes) =
        RequestSubject(currentUser, currentVisitor, attributes)

    @Bean
    @ConditionalOnMissingBean
    fun ofrepApi(flags: FeatureFlags, experiments: Experiments, jsonMapper: JsonMapper) =
        OfrepApi(flags, experiments, jsonMapper)

    @Bean
    @ConditionalOnMissingBean(name = ["featureManagementEndpoints"])
    fun featureManagementEndpoints(api: OfrepApi): RouterFunction<ServerResponse> = RouterFunctions.route()
        .POST(OFREP_BULK_PATH, api::evaluate)
        .POST(EXPOSURES_PATH, api::reportExposure)
        .build()
}
