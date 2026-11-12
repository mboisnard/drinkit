package com.drinkit.security.infra

import com.drinkit.rate.limit.RateLimitDecision
import com.drinkit.rate.limit.RateLimitExceededException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.web.ErrorResponseException
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Hands every refusal of the filter chain to Spring MVC, which answers it with the problem detail of any other error.
 * Every refused sign-in gets the same 401, and every limited one the same 429, so that a caller cannot tell an
 * unknown login from a wrong code: the exception handed over never carries the refusal's own message.
 *
 * OWASP: ASVS 5.0 V6.3.8, Authentication Cheat Sheet (generic error messages).
 */
internal class SecurityErrorResponses(private val resolver: HandlerExceptionResolver) :
    AuthenticationEntryPoint,
    AccessDeniedHandler,
    AuthenticationFailureHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = refuse(request, response, ErrorResponseException(HttpStatus.UNAUTHORIZED))

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) = refuse(request, response, ErrorResponseException(HttpStatus.UNAUTHORIZED))

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = refuse(request, response, ErrorResponseException(HttpStatus.FORBIDDEN))

    fun tooManyRequests(
        request: HttpServletRequest,
        response: HttpServletResponse,
        refused: RateLimitDecision.Refused,
    ) = refuse(request, response, RateLimitExceededException(refused.rule, refused.retryAfter))

    private fun refuse(request: HttpServletRequest, response: HttpServletResponse, refusal: ErrorResponseException) {
        resolver.resolveException(request, response, null, refusal)
    }
}
