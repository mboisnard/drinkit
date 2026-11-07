package com.drinkit.security.infra.chain

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.web.filter.OncePerRequestFilter

internal const val USER_ID_LOG_KEY = "user.id"

// The id, never the email: a log is read by more people than the database
internal class UserIdLoggingFilter(private val holder: SecurityContextHolderStrategy) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val userId = holder.context.authentication
            ?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }
            ?.name

        if (userId == null) {
            chain.doFilter(request, response)
            return
        }

        MDC.putCloseable(USER_ID_LOG_KEY, userId).use { chain.doFilter(request, response) }
    }
}
