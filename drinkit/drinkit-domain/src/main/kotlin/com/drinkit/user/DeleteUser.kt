package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.event.sourcing.transaction.RetryableTransactional
import com.drinkit.user.DeleteUser.Result.Forbidden
import com.drinkit.user.DeleteUser.Result.Success
import com.drinkit.user.DeleteUser.Result.UserNotFound
import com.drinkit.user.UserDeletionDecider.Decision.AlreadyDeleted
import com.drinkit.user.UserDeletionDecider.Decision.EventToPersist
import com.drinkit.user.UserDeletionDecider.Decision.Unauthorized
import com.drinkit.user.core.Deleted
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.UserEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class DeleteUserCommand(
    val author: Author.Connected,
)

@Service
@RetryableTransactional
@Usecase @ImperativeShell
class DeleteUser(
    private val userEvents: UserEvents,
    private val clock: Clock,
) {
    sealed interface Result {
        object Success : Result
        object UserNotFound : Result
        object Forbidden : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(userId: UserId, command: DeleteUserCommand): Result {
        logger.debug { "Deleting user $userId with command $command" }

        val decision = userEvents.findAllBy(userId)
            ?.let {
                UserDeletionDecider.decide(
                    decision = UserDecision.from(it),
                    command = command,
                    date = OffsetDateTime.now(clock),
                )
            }
            ?: return UserNotFound

        return when (decision) {
            is EventToPersist -> {
                userEvents.save(decision.event)
                logger.info { "User $userId has been deleted by ${command.author}" }
                Success
            }
            AlreadyDeleted -> Success
            Unauthorized -> Forbidden
        }
    }
}

@Aggregate @FunctionalCore
internal object UserDeletionDecider {

    sealed interface Decision {
        data class EventToPersist(val event: Deleted) : Decision
        object AlreadyDeleted : Decision
        object Unauthorized : Decision
    }

    fun decide(
        decision: UserDecision,
        command: DeleteUserCommand,
        date: OffsetDateTime,
    ): Decision {
        if (!decision.canEdit(command.author)) {
            return Unauthorized
        }

        if (decision.isDeleted) {
            return AlreadyDeleted
        }

        return EventToPersist(
            Deleted(
                userId = decision.id,
                date = date,
                author = command.author,
                sequenceId = decision.nextSequenceId,
            )
        )
    }
}