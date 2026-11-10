package com.drinkit.security.infra.rememberme

import com.drinkit.postgresql.toStoredTime
import com.drinkit.security.generated.jooq.tables.records.RememberMeTokenRecord
import com.drinkit.security.generated.jooq.tables.references.REMEMBER_ME_TOKEN
import org.jooq.DSLContext
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** [series] identifies a remembered browser for its whole life, while its token changes at each visit. */
internal data class RememberMeSeries(
    val series: String,
    val userId: String,
    val tokenHash: String,
    val previousTokenHash: String?,
    val rotatedAt: Instant,
    val createdAt: Instant,
)

/**
 * A rotation only succeeds from the token it expects, so that two requests with one token cannot both rotate it. Each
 * method runs in a transaction of its own: the remember-me filter runs outside any use case.
 *
 * OWASP: ASVS 5.0 V15.4.2, Session Management Cheat Sheet (atomic rotation).
 */
@Transactional
internal class RememberMeTokens(private val dsl: DSLContext) {

    fun create(series: RememberMeSeries, userAgent: String?) {
        dsl.executeInsert(
            RememberMeTokenRecord(
                series = series.series,
                userId = series.userId,
                tokenHash = series.tokenHash,
                previousTokenHash = series.previousTokenHash,
                rotatedAt = series.rotatedAt.toStoredTime(),
                createdAt = series.createdAt.toStoredTime(),
                userAgent = userAgent,
            ),
        )
    }

    @Transactional(readOnly = true)
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

    fun rotate(series: String, currentHash: String, newHash: String, at: Instant): Boolean = dsl
        .update(REMEMBER_ME_TOKEN)
        .set(REMEMBER_ME_TOKEN.TOKEN_HASH, newHash)
        .set(REMEMBER_ME_TOKEN.PREVIOUS_TOKEN_HASH, REMEMBER_ME_TOKEN.TOKEN_HASH)
        .set(REMEMBER_ME_TOKEN.ROTATED_AT, at.toStoredTime())
        .where(REMEMBER_ME_TOKEN.SERIES.eq(series))
        .and(REMEMBER_ME_TOKEN.TOKEN_HASH.eq(currentHash))
        .execute() == 1

    fun delete(series: String) {
        dsl.deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.SERIES.eq(series)).execute()
    }

    fun deleteAllOf(userId: String) {
        dsl.deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.USER_ID.eq(userId)).execute()
    }

    fun deleteCreatedBefore(instant: Instant) {
        dsl.deleteFrom(REMEMBER_ME_TOKEN).where(REMEMBER_ME_TOKEN.CREATED_AT.lt(instant.toStoredTime())).execute()
    }
}
