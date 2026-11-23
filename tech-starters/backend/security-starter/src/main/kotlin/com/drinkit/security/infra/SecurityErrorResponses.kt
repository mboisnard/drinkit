package com.drinkit.security.infra

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.firewall.RequestRejectedException
import org.springframework.security.web.firewall.RequestRejectedHandler
import org.springframework.web.ErrorResponseException
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Hands every refusal of the filter chain, and of its firewall, to Spring MVC, which answers it with the problem detail
 * of any other error. The exception handed over never carries the refusal's own message.
 *
 * OWASP: ASVS 5.0 V6.3.8, Authentication Cheat Sheet (generic error messages).
 */
internal class SecurityErrorResponses(private val resolver: HandlerExceptionResolver) :
    AuthenticationEntryPoint,
    AccessDeniedHandler,
    RequestRejectedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = refuse(request, response, ErrorResponseException(HttpStatus.UNAUTHORIZED))

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = refuse(request, response, ErrorResponseException(HttpStatus.FORBIDDEN))

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        requestRejectedException: RequestRejectedException,
    ) = refuse(request, response, ErrorResponseException(HttpStatus.BAD_REQUEST))

    private fun refuse(request: HttpServletRequest, response: HttpServletResponse, refusal: ErrorResponseException) {
        resolver.resolveException(request, response, null, refusal)
    }
}
