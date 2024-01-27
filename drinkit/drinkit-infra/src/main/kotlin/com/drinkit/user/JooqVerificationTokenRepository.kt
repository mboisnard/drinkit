package com.drinkit.user

import com.drinkit.user.registration.VerificationToken
import com.drinkit.user.registration.VerificationTokenRepository
import org.springframework.stereotype.Repository

@Repository
internal class JooqVerificationTokenRepository: VerificationTokenRepository {
    override fun create(verificationToken: VerificationToken) {
        TODO("Not yet implemented")
    }
}