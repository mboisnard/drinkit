package com.drinkit.user

import com.drinkit.user.core.UserId
import com.drinkit.user.core.VerificationToken
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.random.Random

@Component
class GenerateVerificationToken(
    private val clock: Clock,
    private val random: Random = Random.Default,
) {
    companion object {
        private const val TOKEN_VALIDITY_IN_HOURS = 24
        private const val TOKEN_LENGTH = 6
    }

    fun invoke(userId: UserId): VerificationToken {
        return VerificationToken(
            userId = userId,
            value = generateRandomString(TOKEN_LENGTH),
            expiryDate = calculateExpirationFromNow(),
        )
    }

    private fun generateRandomString(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        return (1..length)
            .map { characters.random(random) }
            .joinToString("")
    }

    private fun calculateExpirationFromNow(): OffsetDateTime =
        OffsetDateTime.now(clock).plusHours(TOKEN_VALIDITY_IN_HOURS.toLong())
}