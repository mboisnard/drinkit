package com.drinkit.user.spi

import com.drinkit.user.core.UserId
import com.drinkit.user.core.VerificationToken

class InMemoryVerificationTokens : VerificationTokens {

    private val verificationTokens: MutableMap<UserId, VerificationToken> = mutableMapOf()

    override fun saveOrUpdate(verificationToken: VerificationToken): VerificationToken {
        verificationTokens[verificationToken.userId] = verificationToken
        return verificationToken
    }

    override fun findBy(userId: UserId, token: String): VerificationToken? =
        verificationTokens[userId]
            ?.takeIf { it.value == token }

    override fun deleteBy(userId: UserId): Int {
        val oldValue = verificationTokens.remove(userId)
        return if (oldValue != null) 1 else 0
    }

    fun count(): Int = verificationTokens.size
}