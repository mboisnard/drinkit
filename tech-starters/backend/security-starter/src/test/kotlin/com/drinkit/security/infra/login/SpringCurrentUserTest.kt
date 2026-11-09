package com.drinkit.security.infra.login

import com.drinkit.security.CurrentUserTestContract
import com.drinkit.security.infra.events.SecurityEventLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

internal class SpringCurrentUserTest : CurrentUserTestContract() {

    private val holder = SecurityContextHolder.getContextHolderStrategy()

    override val currentUser = SpringCurrentUser(
        holder,
        accounts,
        NullAuthenticatedSessionStrategy(),
        HttpSessionSecurityContextRepository(),
        SecurityEventLogger(),
    )

    @BeforeEach
    fun openRequest() {
        RequestContextHolder.setRequestAttributes(
            ServletRequestAttributes(MockHttpServletRequest(), MockHttpServletResponse()),
        )
    }

    @AfterEach
    fun closeRequest() {
        RequestContextHolder.resetRequestAttributes()
        holder.clearContext()
    }
}
