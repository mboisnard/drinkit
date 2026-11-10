package com.drinkit.security.infra

import com.drinkit.rate.limit.RateLimitDecision
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

/**
 * Every refused sign-in gets the same 401, and every limited one the same 429, so that a caller cannot tell an
 * unknown login from a wrong code.
 *
 * OWASP: ASVS 5.0 V6.3.8, Authentication Cheat Sheet (generic error messages).
 */
internal class SecurityErrorResponses(private val jsonMapper: JsonMapper) :
    AuthenticationEntryPoint,
    AccessDeniedHandler,
    AuthenticationFailureHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = write(request, response, HttpStatus.UNAUTHORIZED)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) = write(request, response, HttpStatus.UNAUTHORIZED)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = write(request, response, HttpStatus.FORBIDDEN)

    fun tooManyRequests(
        request: HttpServletRequest,
        response: HttpServletResponse,
        refused: RateLimitDecision.Refused,
    ) {
        response.setHeader(HttpHeaders.RETRY_AFTER, refused.retryAfterSeconds.toString())
        write(request, response, HttpStatus.TOO_MANY_REQUESTS)
    }

    fun tooManyRequests(request: HttpServletRequest, refused: RateLimitDecision.Refused): ServerResponse =
        ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, refused.retryAfterSeconds.toString())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem(request, HttpStatus.TOO_MANY_REQUESTS))

    private fun write(request: HttpServletRequest, response: HttpServletResponse, status: HttpStatus) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE

        jsonMapper.writeValue(response.outputStream, problem(request, status))
    }

    private fun problem(request: HttpServletRequest, status: HttpStatus) = buildMap {
        put("type", "about:blank")
        put("title", status.reasonPhrase)
        put("status", status.value())
        put("instance", request.requestURI)
        // Put there by Micrometer Tracing, when the application has it: the logs of the request share it
        MDC.get("traceId")?.let { put("traceId", it) }
    }
}
