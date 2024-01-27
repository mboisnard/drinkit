package com.drinkit.user

import com.drinkit.generated.jooq.tables.references.VERIFICATION_TOKEN
import com.drinkit.user.registration.VerificationToken
import com.drinkit.user.registration.VerificationTokenRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
internal class JooqVerificationTokenRepository(
    private val dslContext: DSLContext,
): VerificationTokenRepository {

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
}