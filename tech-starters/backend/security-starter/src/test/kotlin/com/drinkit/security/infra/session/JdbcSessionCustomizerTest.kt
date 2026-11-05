package com.drinkit.security.infra.session

import com.drinkit.security.infra.SecurityStarterProperties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.transaction.support.TransactionOperations
import java.time.Duration

internal class JdbcSessionCustomizerTest {

    private val repository = JdbcIndexedSessionRepository(JdbcTemplate(), TransactionOperations.withoutTransaction())

    // JdbcIndexedSessionRepository's own session type is package-private
    private val sessions: SessionRepository<out Session> = repository

    @Test
    fun `gives a new session the idle timeout of the starter`() {
        // Given
        JdbcSessionCustomizer(
            SecurityStarterProperties.Session(idleTimeout = Duration.ofMinutes(10)),
        ).customize(repository)

        // When
        val session = sessions.createSession()

        // Then
        session.maxInactiveInterval shouldBe Duration.ofMinutes(10)
    }

    @Test
    fun `gives a session an id of 256 random bits, safe in a cookie`() {
        // Given
        JdbcSessionCustomizer(SecurityStarterProperties.Session()).customize(repository)

        // When
        val ids = List(2) { sessions.createSession().id }

        // Then
        ids.forEach { it shouldMatch Regex("[A-Za-z0-9_-]{43}") }
        ids[0] shouldNotBe ids[1]
    }
}
