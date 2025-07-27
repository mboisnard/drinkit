package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.user.CompleteProfileInformation.Result.Forbidden
import com.drinkit.user.CompleteProfileInformation.Result.Success
import com.drinkit.user.CompleteProfileInformation.Result.UserNotFound
import com.drinkit.user.ProfileCompletionDecider.Decision.AlreadyCompleted
import com.drinkit.user.ProfileCompletionDecider.Decision.EventToPersist
import com.drinkit.user.ProfileCompletionDecider.Decision.Unauthorized
import com.drinkit.user.ProfileCompletionDecider.Decision.ValidationFailed
import com.drinkit.user.core.ProfileCompleted
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.User
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class CompleteProfileInformationCommand(
    val author: Author.Connected,
    val profileInformation: ProfileInformation,
)

@Transactional // TODO nested Transactional
@Service
@Usecase @ImperativeShell
class CompleteProfileInformation(
    private val userEvents: UserEvents,
    private val users: Users,
    private val clock: Clock,
) {
    sealed interface Result {
        data class Success(val user: User) : Result
        object UserNotFound : Result
        object Forbidden : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(userId: UserId, command: CompleteProfileInformationCommand): Result {
        logger.debug { "Completing profile information for user $userId with command $command" }

        val decision = userEvents.findAllBy(userId)
            ?.let {
                ProfileCompletionDecider.invoke(
                    decision = UserDecision.from(it),
                    command = command,
                    date = OffsetDateTime.now(clock),
                )
            }
            ?: return UserNotFound

        return when (decision) {
            is EventToPersist -> Success(userEvents.save(decision.event))
            AlreadyCompleted -> Success(users.findEnabledBy(userId)!!)
            is ValidationFailed -> throw IllegalStateException("Validation failed: ${decision.errors}")
            Unauthorized -> Forbidden
        }
    }
}

@Aggregate @FunctionalCore
internal object ProfileCompletionDecider {

    sealed interface Decision {
        data class EventToPersist(val event: ProfileCompleted) : Decision
        object AlreadyCompleted : Decision
        data class ValidationFailed(val errors: List<String>) : Decision
        object Unauthorized : Decision
    }

    fun invoke(
        decision: UserDecision,
        command: CompleteProfileInformationCommand,
        date: OffsetDateTime,
    ): Decision {
        if (!decision.canEdit(command.author)) {
            return Unauthorized
        }

        if (decision.isProfileCompleted) {
            return AlreadyCompleted
        }

        val validationErrors = command.profileInformation.validate()
        if (validationErrors.isNotEmpty()) {
            return ValidationFailed(validationErrors)
        }

        return EventToPersist(
            ProfileCompleted(
                userId = decision.id,
                date = date,
                author = command.author,
                sequenceId = decision.nextSequenceId,
                profile = command.profileInformation,
            )
        )
    }
}