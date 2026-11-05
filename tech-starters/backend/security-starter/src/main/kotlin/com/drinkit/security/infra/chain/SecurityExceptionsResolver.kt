package com.drinkit.security.infra.chain

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

/**
 * Lets a security exception thrown by a controller, such as a refusal of `@PreAuthorize` or `CurrentUser.get()` for
 * an anonymous caller, reach the security filters, which answer 401 or 403. Without it, a catch-all exception
 * handler of the application would turn the refusal into a 500. Not a `@ControllerAdvice`, which a component scan of
 * the application would register even when the starter is switched off.
 *
 * OWASP: ASVS 5.0 V16.5.1, Error Handling Cheat Sheet (generic refusal, never a 500).
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
