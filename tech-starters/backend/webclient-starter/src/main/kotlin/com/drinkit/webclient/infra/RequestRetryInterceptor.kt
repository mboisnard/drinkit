package com.drinkit.webclient.infra

import com.drinkit.idempotency.CurrentIdempotencyKey
import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * Sends again a safe request that got no answer. A POST or a PUT would apply twice, so is sent once, unless the API
 * of its group honours the `Idempotency-Key` header and the request carries one: its own, or one derived from the
 * key of the request or the event being handled, the same when they are replayed.
 */
internal class RequestRetryInterceptor(retry: WebClientProperties.Retry, private val honoursIdempotencyKey: Boolean) :
    ClientHttpRequestInterceptor {

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
    ): ClientHttpResponse {
        val keyed = request.method !in SAFE_METHODS && honoursIdempotencyKey
        if (keyed) addDerivedKeyIfMissing(request)

        val retryable = request.method in SAFE_METHODS ||
            (keyed && request.headers.containsHeader(CurrentIdempotencyKey.HEADER))
        if (!retryable) return execution.execute(request, body)

        // The caller expects the I/O failure, which RestClient turns into a ResourceAccessException
        try {
            return retryTemplate.execute { execution.execute(request, body) }
        } catch (exhausted: RetryException) {
            throw exhausted.lastFailureWithEarlierOnes()
        }
    }

    private fun addDerivedKeyIfMissing(request: HttpRequest) {
        if (request.headers.containsHeader(CurrentIdempotencyKey.HEADER)) return

        CurrentIdempotencyKey.derive("${request.method} ${request.uri}")
            ?.let { request.headers.set(CurrentIdempotencyKey.HEADER, it) }
    }

    private fun RetryException.lastFailureWithEarlierOnes(): Throwable =
        cause.also { last -> suppressed.forEach(last::addSuppressed) }

    private companion object {
        val SAFE_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)
    }
}
