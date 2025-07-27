package com.drinkit.user.spi

import com.drinkit.user.core.UserId
import com.drinkit.user.core.VerificationToken

interface VerificationTokens {

    fun saveOrUpdate(verificationToken: VerificationToken): VerificationToken?

    fun findBy(userId: UserId, token: String): VerificationToken?

    fun deleteBy(userId: UserId): Int
}