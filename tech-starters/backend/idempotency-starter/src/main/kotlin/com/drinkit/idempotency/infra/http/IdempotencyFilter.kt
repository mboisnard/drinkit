package com.drinkit.idempotency.infra.http

import com.drinkit.idempotency.CurrentIdempotencyKey
import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.Idempotent
import com.drinkit.idempotency.IdempotentResult
import com.drinkit.kotlin.sha256
import com.drinkit.security.CurrentUser
import com.drinkit.security.CurrentVisitor
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.ErrorResponseException
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.ContentCachingResponseWrapper

private const val KEY_IN_USE = "A request with this Idempotency-Key is still running"
private const val KEY_REUSED = "This Idempotency-Key was sent with another request"
private const val KEY_INVALID = "An Idempotency-Key holds 1 to 255 characters, not only blanks"
private const val BODY_TOO_LARGE = "A request with an Idempotency-Key carries at most 64 KB"
private const val MAX_KEY_LENGTH = 255

/**
 * Runs the requests of an [Idempotent] endpoint once per key and per user, or else per visitor. A servlet filter
 * rather than a proxy around the controller, which Kotlin's final classes would refuse.
 */
internal class IdempotencyFilter(
    private val idempotency: Idempotency,
    private val transaction: TransactionTemplate,
    private val handlerMapping: RequestMappingHandlerMapping,
    private val currentUser: CurrentUser,
    private val currentVisitor: CurrentVisitor,
    private val resolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val key = request.getHeader(CurrentIdempotencyKey.HEADER)
        val endpoint = key?.let { idempotentEndpointOf(request) }

        if (endpoint == null) {
            chain.doFilter(request, response)
            return
        }

        val replayable = ReplayableBodyRequest(request)
        when {
            key.isBlank() || key.length > MAX_KEY_LENGTH -> refuse(
                request,
                response,
                HttpStatus.BAD_REQUEST,
                KEY_INVALID,
            )

            replayable.isTooLarge -> refuse(request, response, HttpStatus.CONTENT_TOO_LARGE, BODY_TOO_LARGE)

            else -> runOnce(key, endpoint, replayable, response, chain)
        }
    }

    private fun runOnce(
        key: String,
        endpoint: String,
        request: ReplayableBodyRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val captured = ContentCachingResponseWrapper(response)

        // One transaction for the endpoint and its key: its writes and its answer commit together, or neither does
        val result = try {
            transaction.execute { status ->
                idempotency.once(
                    scope = "http:${caller()}:$endpoint",
                    key = key,
                    fingerprint = fingerprintOf(request),
                    record = RecordedResponse::toBytes,
                ) {
                    chain.doFilter(request, captured)
                    if (status.isRollbackOnly) throw NotKept()

                    RecordedResponse.of(captured) ?: throw NotKept()
                }
            }
        } catch (_: NotKept) {
            null
        }

        when (result) {
            null, is IdempotentResult.Executed -> captured.copyBodyToResponse()

            is IdempotentResult.AlreadyDone -> RecordedResponse.from(checkNotNull(result.recorded)).replayOn(response)

            // Not the end of the lease: most requests end in a second
            is IdempotentResult.InFlight -> refuse(request, response, HttpStatus.CONFLICT, KEY_IN_USE, retryAfter = "1")

            IdempotentResult.Mismatch -> refuse(request, response, HttpStatus.UNPROCESSABLE_CONTENT, KEY_REUSED)
        }
    }

    private fun idempotentEndpointOf(request: HttpServletRequest): String? {
        val method = runCatching { handlerMapping.getHandler(request)?.handler as? HandlerMethod }.getOrNull()

        return method?.takeIf { it.hasMethodAnnotation(Idempotent::class.java) }
            ?.let { "${it.beanType.simpleName}.${it.method.name}" }
    }

    private fun fingerprintOf(request: ReplayableBodyRequest) = with(request) {
        "$method $requestURI?${queryString.orEmpty()}\n${body.decodeToString()}".sha256()
    }

    private fun caller() = currentUser.orNull()?.let { "user:${it.id}" } ?: "visitor:${currentVisitor.id()}"

    private fun refuse(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        detail: String,
        retryAfter: String? = null,
    ) {
        val refusal = ErrorResponseException(status).apply {
            setDetail(detail)
            retryAfter?.let { headers.set(HttpHeaders.RETRY_AFTER, it) }
        }
        resolver.resolveException(request, response, null, refusal)
    }

    /**
     * An answer the next try may change: a server error, a request to slow down, or a refusal whose writes roll
     * back. Thrown, it rolls the writes back and frees the key.
     */
    private class NotKept : RuntimeException()
}
