package com.drinkit.security.infra.chain

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import tools.jackson.databind.json.JsonMapper

/**
 * Writes the JSON body of every security refusal in the RFC 9457 ProblemDetail format, as the rest of the API does.
 * Spring Security calls it as the authentication entry point (an anonymous caller gets a 401), as the access-denied
 * handler (a 403), and as the sign-in failure handler: every refused sign-in gets the same 401, so that a caller
 * cannot tell an unknown login from a wrong code. The body never says why the request was refused.
 *
 * OWASP: ASVS 5.0 V6.3.8, V16.5.1, Authentication Cheat Sheet (generic error messages).
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

    private fun write(request: HttpServletRequest, response: HttpServletResponse, status: HttpStatus) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE

        jsonMapper.writeValue(
            response.outputStream,
            mapOf(
                "type" to "about:blank",
                "title" to status.reasonPhrase,
                "status" to status.value(),
                "instance" to request.requestURI,
            ),
        )
    }
}
