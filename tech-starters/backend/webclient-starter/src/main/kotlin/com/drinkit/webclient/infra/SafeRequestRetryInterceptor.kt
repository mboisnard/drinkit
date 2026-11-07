package com.drinkit.webclient.infra

import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

// Replaying a POST or a PUT would not be idempotent, so those are executed once whatever happens
internal class SafeRequestRetryInterceptor(retry: WebClientProperties.Retry) : ClientHttpRequestInterceptor {

    private val retryTemplate = RetryTemplate(
        RetryPolicy.builder()
            .maxRetries(retry.maxRetries)
            .delay(retry.delay)
            .build(),
    )

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse = if (request.method in SAFE_METHODS) {
        // The caller expects the I/O failure, which RestClient turns into a ResourceAccessException
        try {
            retryTemplate.execute { execution.execute(request, body) }
        } catch (exhausted: RetryException) {
            throw exhausted.lastFailureWithEarlierOnes()
        }
    } else {
        execution.execute(request, body)
    }

    private fun RetryException.lastFailureWithEarlierOnes(): Throwable =
        cause.also { last -> suppressed.forEach(last::addSuppressed) }

    private companion object {
        val SAFE_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)
    }
}
