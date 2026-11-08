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

internal const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

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
    ): ClientHttpResponse = if (request.method in SAFE_METHODS || (honoursIdempotencyKey && hasKey(request))) {
        // The caller expects the I/O failure, which RestClient turns into a ResourceAccessException
        try {
            retryTemplate.execute { execution.execute(request, body) }
        } catch (exhausted: RetryException) {
            throw exhausted.lastFailureWithEarlierOnes()
        }
    } else {
        execution.execute(request, body)
    }

    private fun hasKey(request: HttpRequest): Boolean {
        if (!request.headers.containsHeader(IDEMPOTENCY_KEY_HEADER)) {
            CurrentIdempotencyKey.derive("${request.method} ${request.uri}")
                ?.let { request.headers.set(IDEMPOTENCY_KEY_HEADER, it) }
        }

        return request.headers.containsHeader(IDEMPOTENCY_KEY_HEADER)
    }

    private fun RetryException.lastFailureWithEarlierOnes(): Throwable =
        cause.also { last -> suppressed.forEach(last::addSuppressed) }

    private companion object {
        val SAFE_METHODS = setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)
    }
}
