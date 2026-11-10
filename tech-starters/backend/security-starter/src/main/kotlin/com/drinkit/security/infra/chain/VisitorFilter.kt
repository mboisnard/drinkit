package com.drinkit.security.infra.chain

import com.drinkit.security.CurrentVisitor
import com.drinkit.security.IpAddress
import com.drinkit.security.infra.SecurityStarterProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.UUID

internal const val VISITOR_ID_LOG_KEY = "visitor.id"

internal val VISITOR_ID_ATTRIBUTE = CurrentVisitor::class.java.name

/** Reads the visitor of the cookie, or a new one, and gives the cookie [validity] more at each request. */
internal class VisitorFilter(private val cookies: SecurityStarterProperties.Cookie, private val validity: Duration) :
    OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val visitorId = request.cookies?.firstOrNull { it.name == cookies.visitor }?.value?.takeIf(::isUuid)
            ?: UUID.randomUUID().toString()

        request.setAttribute(VISITOR_ID_ATTRIBUTE, visitorId)
        response.addCookie(cookies.forSite(cookies.visitor, visitorId, validity.toSeconds().toInt()))

        MDC.putCloseable(VISITOR_ID_LOG_KEY, visitorId).use { chain.doFilter(request, response) }
    }

    private fun isUuid(value: String) = runCatching { UUID.fromString(value) }.isSuccess
}

internal class RequestCurrentVisitor : CurrentVisitor {

    override fun id(): String = RequestContextHolder.currentRequestAttributes()
        .getAttribute(VISITOR_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) as String

    override fun ipAddress() =
        IpAddress((RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request.remoteAddr)
}
