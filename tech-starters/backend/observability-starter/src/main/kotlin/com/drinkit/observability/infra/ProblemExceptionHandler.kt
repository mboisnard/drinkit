package com.drinkit.observability.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Answers every error of Spring MVC with a problem detail carrying the traceId of the request. Abstract, so that
 * the component scan of an application never registers it a second time.
 */
@ControllerAdvice
@Suppress("AbstractClassCanBeConcreteClass")
internal abstract class ProblemExceptionHandler : ResponseEntityExceptionHandler() {

    // Hides the commons-logging logger of ResponseEntityExceptionHandler, which keeps using its own
    @Suppress("PROPERTY_HIDES_JAVA_FIELD")
    private val logger = KotlinLogging.logger { }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: Exception, request: WebRequest): ResponseEntity<Any>? {
        logger.warn { ex.message }

        return problem(ex, HttpStatus.BAD_REQUEST, request)
    }

    // The body is final here, including the problem Spring builds from an ErrorResponse
    override fun createResponseEntity(
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        // Put there by Micrometer Tracing: the logs of the request share it
        MDC.get("traceId")?.let { traceId -> (body as? ProblemDetail)?.setProperty("traceId", traceId) }

        return super.createResponseEntity(body, headers, statusCode, request)
    }

    private fun problem(ex: Exception, status: HttpStatus, request: WebRequest) =
        handleExceptionInternal(ex, ProblemDetail.forStatus(status), HttpHeaders(), status, request)
}
