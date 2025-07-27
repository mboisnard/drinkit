package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.event.sourcing.transaction.RetryableTransactional
import com.drinkit.user.ConfirmVerificationToken.Result.Forbidden
import com.drinkit.user.ConfirmVerificationToken.Result.NotFound
import com.drinkit.user.ConfirmVerificationToken.Result.Success
import com.drinkit.user.ConfirmVerificationToken.Result.TokenExpired
import com.drinkit.user.VerificationTokenDecider.Decision.AlreadyVerified
import com.drinkit.user.VerificationTokenDecider.Decision.Expired
import com.drinkit.user.VerificationTokenDecider.Decision.Unauthorized
import com.drinkit.user.VerificationTokenDecider.Decision.EventToPersist
import com.drinkit.user.core.User
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserId
import com.drinkit.user.core.Verified
import com.drinkit.user.core.VerificationToken
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.Users
import com.drinkit.user.spi.VerificationTokens
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class ConfirmVerificationTokenCommand(
    val author: Author.Connected,
    val token: String,
)

@Service
@RetryableTransactional
@Usecase @ImperativeShell
class ConfirmVerificationToken(
    private val userEvents: UserEvents,
    private val users: Users,
    private val verificationTokens: VerificationTokens,
    private val clock: Clock,
) {
    sealed interface Result {
        data class Success(val user: User) : Result
        object Forbidden : Result
        object NotFound : Result
        object TokenExpired : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(userId: UserId, command: ConfirmVerificationTokenCommand): Result {
        logger.debug { "Confirming verification token for user $userId" }

        val decision = run {
            val verificationToken = verificationTokens.findBy(userId, command.token)
                ?: return@run null

            val userHistory = userEvents.findAllBy(userId)
                ?: return@run null

            VerificationTokenDecider.decide(
                decision = UserDecision.from(userHistory),
                command = command,
                foundToken = verificationToken,
                date = OffsetDateTime.now(clock),
            )
        } ?: return NotFound

        return when (decision) {
            is EventToPersist -> {
                verificationTokens.deleteBy(userId)
                Success(userEvents.save(decision.event))
            }
            AlreadyVerified -> Success(users.findEnabledBy(userId)!!)
            is Expired -> {
                verificationTokens.deleteBy(userId)
                TokenExpired
            }
            Unauthorized -> Forbidden
        }
    }
}

@FunctionalCore
internal object VerificationTokenDecider {

    sealed interface Decision {
        data class EventToPersist(val event: Verified) : Decision
        object AlreadyVerified : Decision
        object Expired : Decision
        object Unauthorized : Decision
    }

    fun decide(
        decision: UserDecision,
        command: ConfirmVerificationTokenCommand,
        foundToken: VerificationToken,
        date: OffsetDateTime,
    ): Decision {
        if (!decision.canEdit(command.author)) {
            return Unauthorized
        }

        if (date.isAfter(foundToken.expiryDate)) {
            return Expired
        }

        if (decision.isVerified) {
            return AlreadyVerified
        }

        return EventToPersist(
            Verified(
                userId = decision.id,
                date = date,
                author = command.author,
                sequenceId = decision.nextSequenceId,
            )
        )
    }
}