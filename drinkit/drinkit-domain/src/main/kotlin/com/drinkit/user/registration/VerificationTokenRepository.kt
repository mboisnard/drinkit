package com.drinkit.user.registration

interface VerificationTokenRepository {

    fun createOrUpdate(verificationToken: VerificationToken): VerificationToken?
}