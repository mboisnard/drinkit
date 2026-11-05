package com.drinkit.security.infra.session

import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.randomToken
import org.springframework.session.SessionIdGenerator
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.jdbc.JdbcIndexedSessionRepository

private const val TABLE_NAME = "drinkit_session.spring_session"

// Spring Session writes an attribute as bytes: these queries, from the Spring Session JDBC reference documentation,
// convert the bytes into the JSONB column of the table.
private const val CREATE_ATTRIBUTE_QUERY = """
    INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
    VALUES (?, ?, convert_from(?, 'UTF8')::jsonb)
    """
private const val UPDATE_ATTRIBUTE_QUERY = """
    UPDATE %TABLE_NAME%_ATTRIBUTES
    SET ATTRIBUTE_BYTES = convert_from(?, 'UTF8')::jsonb
    WHERE SESSION_PRIMARY_ID = ?
    AND ATTRIBUTE_NAME = ?
    """

/**
 * Adapts the sessions Spring Session keeps in PostgreSQL to the tables of `drinkit_session`. Their attributes are
 * stored in JSON rather than as serialized Java objects, which a change of class could make unreadable and a forged
 * row could turn into code execution. Their ids hold 256 random bits, and a session ends after its idle timeout or
 * its absolute timeout, whichever comes first, so that a stolen session id does not last forever.
 *
 * OWASP: ASVS 5.0 V7.2.3, V7.3.1, V7.3.2, Session Management Cheat Sheet (id entropy, idle and absolute timeouts).
 */
internal class JdbcSessionCustomizer(private val session: SecurityStarterProperties.Session) :
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

    override fun customize(repository: JdbcIndexedSessionRepository) {
        // First: setTableName builds every query again from the table name, which would undo the queries set below
        repository.setTableName(TABLE_NAME)
        repository.setCreateSessionAttributeQuery(CREATE_ATTRIBUTE_QUERY)
        repository.setUpdateSessionAttributeQuery(UPDATE_ATTRIBUTE_QUERY)
        repository.setGetSessionQuery(getSessionQuery())
        repository.setDefaultMaxInactiveInterval(session.idleTimeout)
        repository.setSessionIdGenerator(SessionIdGenerator(::randomToken))
    }

    // The query of Spring Session, plus a condition on the creation time: Spring Session only ends a session after
    // its idle timeout, and this one no longer finds a session older than the absolute timeout, however active.
    private fun getSessionQuery() = """
        SELECT S.PRIMARY_ID, S.SESSION_ID, S.CREATION_TIME, S.LAST_ACCESS_TIME, S.MAX_INACTIVE_INTERVAL,
            SA.ATTRIBUTE_NAME, SA.ATTRIBUTE_BYTES
        FROM %TABLE_NAME% S
        LEFT JOIN %TABLE_NAME%_ATTRIBUTES SA ON S.PRIMARY_ID = SA.SESSION_PRIMARY_ID
        WHERE S.SESSION_ID = ?
        AND S.CREATION_TIME > (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT - ${session.absoluteTimeout.toMillis()}
        """
}
