package com.drinkit.security.infra.rememberme

import com.drinkit.kotlin.sha256
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.Sessions
import com.drinkit.security.UserAccount
import com.drinkit.security.generated.jooq.DrinkitSession
import com.drinkit.security.generated.jooq.tables.references.REMEMBER_ME_TOKEN
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.login.UserAccountsUserDetailsService
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import jakarta.servlet.http.Cookie
import org.jooq.DSLContext
import org.jooq.ExecuteListener
import org.jooq.Query
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

private const val USER_ID = "64656d6f0000000000000001"
private const val SERIES = "a-series"
private const val TOKEN = "a-token"

@JooqIntegrationTest(schemas = [DrinkitSession::class])
internal class RotatingRememberMeServicesTest {

    private val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
    private val sessions = RecordingSessions()
    private val cookie = SecurityStarterProperties.Cookie(prefix = "", secure = false)

    @Test
    fun `signs nobody out everywhere when the series ends while its token rotates`(dsl: DSLContext) {
        // Given
        RememberMeTokens(dsl)
            .create(RememberMeSeries(SERIES, USER_ID, TOKEN.sha256(), null, now, now), null)
        val services = servicesOn(dsl.endingTheSeriesBeforeItsRotation())

        // When
        val authentication = services.autoLogin(returningBrowser(), MockHttpServletResponse())

        // Then
        authentication.shouldBeNull()
        sessions.revoked.shouldBeEmpty()
    }

    private fun servicesOn(dsl: DSLContext) = RotatingRememberMeServices(
        users = UserAccountsUserDetailsService(
            InMemoryUserAccounts("ada@drinkit.test" to UserAccount(USER_ID, setOf("ROLE_USER"))),
        ),
        rememberMeTokens = RememberMeTokens(dsl),
        sessions = sessions,
        events = SecurityEventLogger(),
        clock = Clock.fixed(now, Clock.systemUTC().zone),
        validity = Duration.ofDays(30),
        cookie = cookie,
    )

    // As a logout on another tab would: the series is gone between its reading and the rotation of its token
    private fun DSLContext.endingTheSeriesBeforeItsRotation(): DSLContext {
        val deleteTheSeries = ExecuteListener.onExecuteStart { context ->
            if (context.query()?.isRotation() == true) {
                deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.SERIES.eq(SERIES)).execute()
            }
        }
        return configuration().deriveAppending(deleteTheSeries).dsl()
    }

    private fun Query.isRotation() = sql.startsWith("update", ignoreCase = true)

    private fun returningBrowser() = MockHttpServletRequest().apply {
        setCookies(Cookie(cookie.rememberMe, Base64.getEncoder().encodeToString("$SERIES:$TOKEN".toByteArray())))
    }

    private class RecordingSessions : Sessions {
        val revoked = mutableListOf<String>()

        override fun revokeAll(userId: String) {
            revoked += userId
        }
    }
}
