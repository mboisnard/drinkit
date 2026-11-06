package com.drinkit.security.infra.code

import com.drinkit.security.generated.jooq.tables.references.SIGN_IN_CODE
import com.drinkit.security.infra.toStoredTime
import org.jooq.DSLContext
import org.jooq.impl.DSL.excluded
import org.jooq.impl.DSL.`when`
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Each method runs in a transaction of its own: the sign-in runs in a filter, outside any use case. */
@Transactional
internal class JooqSignInCodes(private val dsl: DSLContext) : SignInCodes {

    override fun replace(code: SignInCode, now: Instant) {
        // The tries of the earlier code stay while their reset is ahead: a new code never gives new tries
        val triesStillRun = SIGN_IN_CODE.ATTEMPTS_RESET_AT.gt(now.toStoredTime())

        dsl.insertInto(SIGN_IN_CODE)
            .set(SIGN_IN_CODE.LOGIN, code.login)
            .set(SIGN_IN_CODE.CODE_HASH, code.codeHash)
            .set(SIGN_IN_CODE.EXPIRES_AT, code.expiresAt.toStoredTime())
            .set(SIGN_IN_CODE.ATTEMPTS_LEFT, code.attemptsLeft)
            .set(SIGN_IN_CODE.ATTEMPTS_RESET_AT, code.attemptsResetAt.toStoredTime())
            .onConflict(SIGN_IN_CODE.LOGIN)
            .doUpdate()
            .set(SIGN_IN_CODE.CODE_HASH, code.codeHash)
            .set(SIGN_IN_CODE.EXPIRES_AT, code.expiresAt.toStoredTime())
            .set(
                SIGN_IN_CODE.ATTEMPTS_LEFT,
                `when`(triesStillRun, SIGN_IN_CODE.ATTEMPTS_LEFT).otherwise(excluded(SIGN_IN_CODE.ATTEMPTS_LEFT)),
            )
            .set(
                SIGN_IN_CODE.ATTEMPTS_RESET_AT,
                `when`(triesStillRun, SIGN_IN_CODE.ATTEMPTS_RESET_AT)
                    .otherwise(excluded(SIGN_IN_CODE.ATTEMPTS_RESET_AT)),
            )
            .execute()
    }

    override fun tryCode(login: String, codeHash: String, now: Instant): Boolean {
        val code = dsl.selectFrom(SIGN_IN_CODE)
            .where(SIGN_IN_CODE.LOGIN.eq(login))
            .forUpdate()
            .fetchOne()
            ?.let { row ->
                SignInCode(
                    row.login,
                    row.codeHash,
                    row.expiresAt.toInstant(),
                    row.attemptsLeft,
                    row.attemptsResetAt.toInstant(),
                )
            }
            ?: return false

        val accepted = code.accepts(codeHash, now)
        when (val left = code.afterTry(accepted)) {
            null -> dsl.deleteFrom(SIGN_IN_CODE).where(SIGN_IN_CODE.LOGIN.eq(login)).execute()

            else -> dsl.update(SIGN_IN_CODE)
                .set(SIGN_IN_CODE.ATTEMPTS_LEFT, left.attemptsLeft)
                .where(SIGN_IN_CODE.LOGIN.eq(login))
                .execute()
        }
        return accepted
    }

    override fun deleteExpiredAt(now: Instant) {
        dsl.deleteFrom(SIGN_IN_CODE)
            .where(SIGN_IN_CODE.EXPIRES_AT.le(now.toStoredTime()))
            .and(SIGN_IN_CODE.ATTEMPTS_RESET_AT.le(now.toStoredTime()))
            .execute()
    }
}
