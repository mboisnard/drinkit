package com.drinkit.user.registration

import com.drinkit.user.UserId
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import kotlin.random.Random

data class VerificationToken(
    val userId: UserId,
    val token: String,
    val expiryDate: LocalDateTime,
)

@Component
class GenerateVerificationToken(
    private val clock: Clock,
    private val random: Random = Random.Default,
) {
    companion object {
        private const val TOKEN_VALIDITY_IN_HOURS = 24
    }

    operator fun invoke(userId: UserId): VerificationToken {
        return VerificationToken(
            userId = userId,
            token = generateRandomString(length = 6),
            expiryDate = calculateExpirationFromNow(),
        )
    }

    private fun generateRandomString(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        return (1..length)
            .map { characters.random(random) }
            .joinToString("")
    }

    private fun calculateExpirationFromNow(): LocalDateTime =
        LocalDateTime.now(clock).plusHours(TOKEN_VALIDITY_IN_HOURS.toLong())
}
