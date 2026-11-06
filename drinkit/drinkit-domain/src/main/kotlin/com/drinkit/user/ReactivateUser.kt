package com.drinkit.user

import com.drinkit.common.Constants.DELETED_ACCOUNT_RETENTION
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.event.sourcing.transaction.RetryableTransactional
import com.drinkit.user.ReactivateUser.Result.Forbidden
import com.drinkit.user.ReactivateUser.Result.NotDeleted
import com.drinkit.user.ReactivateUser.Result.RetentionOver
import com.drinkit.user.ReactivateUser.Result.UserNotFound
import com.drinkit.user.ReactivationDecider.Decision.EventToPersist
import com.drinkit.user.ReactivationDecider.Decision.Unauthorized
import com.drinkit.user.core.Author
import com.drinkit.user.core.Reactivated
import com.drinkit.user.core.User
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.UserEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class ReactivateUserCommand(val author: Author.Connected)

@Service
@RetryableTransactional
@Usecase
@ImperativeShell
class ReactivateUser(private val userEvents: UserEvents, private val clock: Clock) {
    sealed interface Result {
        data class Reactivated(val user: User) : Result
        object UserNotFound : Result
        object Forbidden : Result
        object NotDeleted : Result
        object RetentionOver : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(userId: UserId, command: ReactivateUserCommand): Result {
        val decision = userEvents.findAllBy(userId)
            ?.let {
                ReactivationDecider.decide(
                    decision = UserDecision.from(it),
                    command = command,
                    date = OffsetDateTime.now(clock),
                )
            }
            ?: return UserNotFound

        return when (decision) {
            is EventToPersist -> Result.Reactivated(userEvents.save(decision.event))
                .also { logger.info { "User $userId has been reactivated" } }

            Unauthorized -> Forbidden

            ReactivationDecider.Decision.NotDeleted -> NotDeleted

            ReactivationDecider.Decision.RetentionOver -> RetentionOver
        }
    }
}

@Aggregate @FunctionalCore
internal object ReactivationDecider {

    sealed interface Decision {
        data class EventToPersist(val event: Reactivated) : Decision
        object Unauthorized : Decision
        object NotDeleted : Decision
        object RetentionOver : Decision
    }

    fun decide(decision: UserDecision, command: ReactivateUserCommand, date: OffsetDateTime): Decision {
        val deletedAt = decision.deletedAt

        return when {
            !decision.canEdit(command.author) -> Unauthorized

            deletedAt == null -> Decision.NotDeleted

            !date.isBefore(deletedAt + DELETED_ACCOUNT_RETENTION) -> Decision.RetentionOver

            else -> EventToPersist(
                Reactivated(
                    userId = decision.id,
                    date = date,
                    author = command.author,
                    sequenceId = decision.nextSequenceId,
                ),
            )
        }
    }
}
