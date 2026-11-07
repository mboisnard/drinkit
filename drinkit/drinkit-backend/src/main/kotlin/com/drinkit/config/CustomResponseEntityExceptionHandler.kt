package com.drinkit.config

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

@ControllerAdvice
internal class CustomResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    // Hides the commons-logging logger of ResponseEntityExceptionHandler, which keeps using its own
    @Suppress("PROPERTY_HIDES_JAVA_FIELD")
    private val logger = KotlinLogging.logger { }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: Exception, request: WebRequest): ResponseEntity<Any>? {
        logger.warn { ex.message }

        return handleExceptionInternal(
            ex,
            ProblemDetail.forStatus(HttpStatus.BAD_REQUEST),
            HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request,
        )
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        // Put there by Micrometer Tracing: the logs of the request share it
        MDC.get("traceId")?.let { traceId -> (body as? ProblemDetail)?.setProperty("traceId", traceId) }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }
}
