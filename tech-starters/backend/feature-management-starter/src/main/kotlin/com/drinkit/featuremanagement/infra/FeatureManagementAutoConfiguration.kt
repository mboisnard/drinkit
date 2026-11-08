package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.Experiments
import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.featuremanagement.infra.flipt.fliptProvider
import com.drinkit.featuremanagement.infra.openfeature.OpenFeatureExperiments
import com.drinkit.featuremanagement.infra.openfeature.OpenFeatureFeatureFlags
import dev.openfeature.sdk.NoOpProvider
import dev.openfeature.sdk.OpenFeatureAPI
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

private const val FLAGS_DOMAIN = "flags"
private const val EXPERIMENTS_DOMAIN = "experiments"

@AutoConfiguration
@EnableConfigurationProperties(FeatureManagementProperties::class)
internal class FeatureManagementAutoConfiguration {

    // Isolated rather than the global singleton, so that two application contexts never share their providers
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    fun openFeatureApi(properties: FeatureManagementProperties): OpenFeatureAPI =
        OpenFeatureAPI.createIsolated().apply {
            setProviderAndWait(
                FLAGS_DOMAIN,
                if (properties.enabled) fliptProvider(properties.flipt) else NoOpProvider(),
            )
            setProviderAndWait(EXPERIMENTS_DOMAIN, NoOpProvider())
        }

    @Bean
    @ConditionalOnMissingBean
    fun featureFlags(openFeatureApi: OpenFeatureAPI, requestSubject: ObjectProvider<RequestSubject>): FeatureFlags =
        OpenFeatureFeatureFlags(openFeatureApi.getClient(FLAGS_DOMAIN)) { requestSubject.getObject().get() }

    @Bean
    @ConditionalOnMissingBean
    fun experiments(
        flags: FeatureFlags,
        openFeatureApi: OpenFeatureAPI,
        requestSubject: ObjectProvider<RequestSubject>,
    ): Experiments = OpenFeatureExperiments(flags, openFeatureApi.getClient(EXPERIMENTS_DOMAIN)) {
        requestSubject.getObject().get()
    }
}
