package com.drinkit.user

import com.drinkit.generated.jooq.tables.records.VerificationTokenRecord
import com.drinkit.generated.jooq.tables.references.VERIFICATION_TOKEN
import com.drinkit.jooq.allFields
import com.drinkit.user.registration.VerificationToken
import com.drinkit.user.registration.VerificationTokens
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
internal class JooqVerificationTokens(
    private val dslContext: DSLContext,
): VerificationTokens {

    override fun createOrUpdate(verificationToken: VerificationToken): VerificationToken? {
        val query = dslContext.insertInto(VERIFICATION_TOKEN)
            .set(VERIFICATION_TOKEN.USER_ID, verificationToken.userId.value)
            .set(VERIFICATION_TOKEN.TOKEN, verificationToken.token)
            .set(VERIFICATION_TOKEN.EXPIRY_DATE, verificationToken.expiryDate)

            .onConflict(VERIFICATION_TOKEN.USER_ID)
            .doUpdate()
            .set(VERIFICATION_TOKEN.TOKEN, verificationToken.token)
            .set(VERIFICATION_TOKEN.EXPIRY_DATE, verificationToken.expiryDate)

        val insertedRowCount = query.execute()

        return if (insertedRowCount != 0) verificationToken else null
    }

    override fun findBy(userId: UserId, token: String): VerificationToken? {
        val query = dslContext.select(
            allFields(VERIFICATION_TOKEN)
        )
            .from(VERIFICATION_TOKEN)
            .where(
                VERIFICATION_TOKEN.USER_ID.eq(userId.value)
                    .and(VERIFICATION_TOKEN.TOKEN.eq(token))
            )

        return query.fetchOne()
            ?.value1()
            ?.toDomain()
    }

    override fun deleteBy(userId: UserId): Int {
        val query = dslContext.deleteFrom(VERIFICATION_TOKEN)
            .where(VERIFICATION_TOKEN.USER_ID.eq(userId.value))
            .limit(1)

        return query.execute()
    }

    private fun VerificationTokenRecord.toDomain(): VerificationToken =
        VerificationToken(
            userId = UserId(userId),
            token = token,
            expiryDate = expiryDate,
        )
}