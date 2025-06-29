package com.drinkit.user.registration

import com.drinkit.user.core.UserId

interface VerificationTokens {

    fun createOrUpdate(verificationToken: VerificationToken): VerificationToken?

    fun findBy(userId: UserId, token: String): VerificationToken?

    fun deleteBy(userId: UserId): Int
}
