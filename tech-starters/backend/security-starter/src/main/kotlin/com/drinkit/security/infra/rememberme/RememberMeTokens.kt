package com.drinkit.security.infra.rememberme

import com.drinkit.security.generated.jooq.tables.records.RememberMeTokenRecord
import com.drinkit.security.generated.jooq.tables.references.REMEMBER_ME_TOKEN
import org.jooq.DSLContext
import org.springframework.transaction.support.TransactionOperations
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * A remembered browser. Its [series] identifies it for its whole life, while its token changes at each visit: only
 * the hash of the current token and of the previous one are kept, so that a stolen table opens no account.
 *
 * OWASP: Session Management Cheat Sheet (tokens stored as hashes).
 */
internal data class RememberMeSeries(
    val series: String,
    val userId: String,
    val tokenHash: String,
    val previousTokenHash: String?,
    val rotatedAt: Instant,
    val createdAt: Instant,
)

/**
 * Stores the remember-me series in PostgreSQL, so that every instance of the application sees the same ones. Each
 * write runs in a transaction of its own, and a rotation only succeeds from the token it expects, so that two
 * requests presenting the same token cannot both rotate it.
 *
 * OWASP: ASVS 5.0 V15.4.2, Session Management Cheat Sheet (hashed tokens, atomic rotation).
 */
internal class RememberMeTokens(private val dsl: DSLContext, private val transactions: TransactionOperations) {

    fun create(series: RememberMeSeries, userAgent: String?) {
        write {
            dsl.executeInsert(
                RememberMeTokenRecord(
                    series = series.series,
                    userId = series.userId,
                    tokenHash = series.tokenHash,
                    previousTokenHash = series.previousTokenHash,
                    rotatedAt = series.rotatedAt.atUtc(),
                    createdAt = series.createdAt.atUtc(),
                    userAgent = userAgent,
                ),
            )
        }
    }

    fun find(series: String): RememberMeSeries? = dsl.selectFrom(REMEMBER_ME_TOKEN)
        .where(REMEMBER_ME_TOKEN.SERIES.eq(series))
        .fetchOne()
        ?.let {
            RememberMeSeries(
                series = it.series,
                userId = it.userId,
                tokenHash = it.tokenHash,
                previousTokenHash = it.previousTokenHash,
                rotatedAt = it.rotatedAt.toInstant(),
                createdAt = it.createdAt.toInstant(),
            )
        }

    /** Rotates the token of [series] from [currentHash] to [newHash], unless another request rotated it first. */
    fun rotate(series: String, currentHash: String, newHash: String, at: Instant): Boolean = write {
        dsl.update(REMEMBER_ME_TOKEN)
            .set(REMEMBER_ME_TOKEN.TOKEN_HASH, newHash)
            .set(REMEMBER_ME_TOKEN.PREVIOUS_TOKEN_HASH, REMEMBER_ME_TOKEN.TOKEN_HASH)
            .set(REMEMBER_ME_TOKEN.ROTATED_AT, at.atUtc())
            .where(REMEMBER_ME_TOKEN.SERIES.eq(series))
            .and(REMEMBER_ME_TOKEN.TOKEN_HASH.eq(currentHash))
            .execute()
    } == 1

    fun delete(series: String) {
        write { dsl.deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.SERIES.eq(series)).execute() }
    }

    fun deleteAllOf(userId: String) {
        write { dsl.deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.USER_ID.eq(userId)).execute() }
    }

    fun deleteCreatedBefore(instant: Instant) {
        write { dsl.deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.CREATED_AT.lt(instant.atUtc())).execute() }
    }

    // The remember-me filter runs before any controller, outside the transaction of a use case, and the connections
    // of the application do not commit on their own: each write needs a transaction of its own.
    private fun write(statement: () -> Int): Int = transactions.execute { statement() }

    // PostgreSQL keeps microseconds and rounds the nanoseconds of an Instant to the nearest one, which could store a
    // later instant than the real one and push an expiry past it. Truncating never stores a later instant.
    private fun Instant.atUtc(): OffsetDateTime = truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)
}
