package com.drinkit.webclient.rest

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import java.time.Duration

/**
 * Retries safe requests. Spring Boot configures timeouts, base URLs and the proxies themselves, but
 * has no built-in retry for HTTP clients, so this is the one piece that stays.
 *
 * Replaying a POST or a PUT would not be idempotent, so those are executed once whatever happens.
 */
internal class SafeRequestRetryInterceptor : ClientHttpRequestInterceptor {

    private val retryTemplate = RetryTemplate(
        RetryPolicy.builder()
            .maxRetries(3)
            .delay(Duration.ofMillis(100))
            .maxDelay(Duration.ofSeconds(3))
            .build(),
    )

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse = if (request.method in SAFE_METHODS) {
        retryTemplate.execute { execution.execute(request, body) }
    } else {
        execution.execute(request, body)
    }

    private companion object {
        val SAFE_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)
    }
}

@TechStarterTool
@Configuration(proxyBeanMethods = false)
class RestClientRetryConfiguration {

    /**
     * Applies the retry policy to every declared HTTP service group.
     */
    @Bean
    fun retryingHttpServiceGroupConfigurer() = RestClientHttpServiceGroupConfigurer { groups ->
        groups.forEachClient { _, clientBuilder ->
            clientBuilder.requestInterceptor(SafeRequestRetryInterceptor())
        }
    }
}
