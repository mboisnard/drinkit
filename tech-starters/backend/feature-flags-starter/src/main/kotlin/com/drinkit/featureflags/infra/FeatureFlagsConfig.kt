package com.drinkit.featureflags.infra

import dev.openfeature.contrib.providers.flipt.FliptProvider
import dev.openfeature.contrib.providers.flipt.FliptProviderConfig
import dev.openfeature.sdk.Client
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.ThreadLocalTransactionContextPropagator
import dev.openfeature.sdk.exceptions.OpenFeatureError
import io.flipt.api.FliptClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@ConfigurationProperties(prefix = "starters.feature-flags.flipt")
internal data class FliptProperties(
    val url: String,
    val namespace: String,
)

@Configuration
@EnableConfigurationProperties(FliptProperties::class)
internal class FeatureFlagsConfig {

    private val logger = KotlinLogging.logger {}

    @Bean
    fun openFeatureAPI(): OpenFeatureAPI = OpenFeatureAPI.getInstance().apply {
        transactionContextPropagator = ThreadLocalTransactionContextPropagator()
    }

    @Bean
    @ConditionalOnWebApplication
    fun featureFlagFilter(openFeatureAPI: OpenFeatureAPI): FilterRegistrationBean<OpenFeatureTransactionContextFilter> {
        val filter = OpenFeatureTransactionContextFilter(openFeatureAPI)

        return FilterRegistrationBean(filter).apply {
            addUrlPatterns("/*")
            order = Ordered.HIGHEST_PRECEDENCE + 10
        }
    }

    @Bean
    fun openFeatureFliptClient(
        properties: FliptProperties,
        openFeatureAPI: OpenFeatureAPI,
    ): Client {
        val fliptDomainName = "flipt"
        val providerConfig = FliptProviderConfig.builder()
            .fliptClientBuilder(FliptClient.builder().url(properties.url))
            .namespace(properties.namespace)
            .build()

        try {
            openFeatureAPI.setProviderAndWait(fliptDomainName, FliptProvider(providerConfig))
        } catch (ex: OpenFeatureError) {
            logger.error(ex) { "Error while setting $fliptDomainName provider" }
        }

        return openFeatureAPI.getClient(fliptDomainName)
    }
}
