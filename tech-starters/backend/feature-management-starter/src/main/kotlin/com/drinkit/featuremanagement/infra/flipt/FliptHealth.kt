package com.drinkit.featuremanagement.infra.flipt

import com.drinkit.featuremanagement.infra.FeatureManagementProperties
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.http.HttpClient
import java.time.Duration

private val HEALTH_TIMEOUT = Duration.ofSeconds(2)

// The Flipt provider reports ready without reaching the server, so its state says nothing of Flipt's health
internal fun fliptHealthIndicator(flipt: FeatureManagementProperties.Flipt): HealthIndicator {
    val requests = JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(HEALTH_TIMEOUT).build())
        .apply { setReadTimeout(HEALTH_TIMEOUT) }
    val client = RestClient.builder().baseUrl(flipt.url).requestFactory(requests).build()

    return HealthIndicator {
        try {
            client.get().uri("/health").retrieve().toBodilessEntity()
            Health.up().build()
        } catch (exception: RestClientException) {
            Health.down(exception).build()
        }
    }
}
