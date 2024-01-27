package com.drinkit.user.registration

interface VerificationTokenRepository {

    fun create(verificationToken: VerificationToken)
}