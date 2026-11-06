package com.drinkit.security.infra.chain

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

/**
 * Without it, a catch-all exception handler of the application turns a security refusal thrown by a controller into a
 * 500. Not a `@ControllerAdvice`, which a component scan would register even with the starter switched off.
 */
internal class SecurityExceptionsResolver :
    HandlerExceptionResolver,
    Ordered {

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        exception: Exception,
    ): ModelAndView? = if (exception is AuthenticationException || exception is AccessDeniedException) {
        throw exception
    } else {
        null
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE
}
