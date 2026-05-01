package com.drinkit.featureflags.infra

import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.MutableContext
import dev.openfeature.sdk.OpenFeatureAPI
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

internal class OpenFeatureTransactionContextFilter(
    private val openFeatureAPI: OpenFeatureAPI,
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val context = MutableContext()

        // TODO Add userId / correlationId to the context

        openFeatureAPI.setTransactionContext(context)

        try {
            filterChain.doFilter(request, response)
        } finally {
            openFeatureAPI.setTransactionContext(ImmutableContext())
        }
    }
}