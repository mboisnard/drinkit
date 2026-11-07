package com.drinkit.security.infra.chain

import io.kotest.matchers.shouldBe
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServlet
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl

internal class UserIdLoggingFilterTest {

    private val holder = SecurityContextHolder.getContextHolderStrategy()

    @AfterEach
    fun clear() {
        holder.clearContext()
    }

    @Test
    fun `logs the id of the signed-in user during the request`() {
        givenSignedIn("user-1")

        logged() shouldBe "user-1"
    }

    @Test
    fun `forgets the user once the request ends`() {
        givenSignedIn("user-1")

        logged()

        MDC.get(USER_ID_LOG_KEY) shouldBe null
    }

    @Test
    fun `logs no user for an anonymous caller`() {
        val roles = AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        holder.context = SecurityContextImpl(AnonymousAuthenticationToken("key", "anonymousUser", roles))

        logged() shouldBe null
    }

    private fun givenSignedIn(userId: String) {
        val roles = AuthorityUtils.createAuthorityList("ROLE_USER")
        holder.context = SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(userId, null, roles))
    }

    private fun logged(): String? {
        var seen: String? = null
        val chain = MockFilterChain(SeenServlet { seen = MDC.get(USER_ID_LOG_KEY) })

        UserIdLoggingFilter(holder).doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        return seen
    }

    private class SeenServlet(private val onRequest: () -> Unit) : HttpServlet() {
        override fun service(request: ServletRequest, response: ServletResponse) = onRequest()
    }
}
