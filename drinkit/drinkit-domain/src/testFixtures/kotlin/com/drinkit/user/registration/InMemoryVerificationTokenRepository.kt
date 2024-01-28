package com.drinkit.user.registration

import com.drinkit.user.UserId

class InMemoryVerificationTokenRepository: VerificationTokenRepository {

    private val verificationTokens: MutableMap<UserId, VerificationToken> = mutableMapOf()

    override fun createOrUpdate(verificationToken: VerificationToken): VerificationToken {
        verificationTokens[verificationToken.userId] = verificationToken
        return verificationToken
    }

    override fun findBy(userId: UserId, token: String): VerificationToken? =
        verificationTokens[userId]
            ?.takeIf { it.token == token }

    override fun deleteBy(userId: UserId): Int {
        val oldValue = verificationTokens.remove(userId)
        return if (oldValue != null) 1 else 0
    }
}