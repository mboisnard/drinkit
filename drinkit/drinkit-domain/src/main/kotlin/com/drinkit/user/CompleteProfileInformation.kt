package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.user.CompleteProfileInformation.Result.Forbidden
import com.drinkit.user.CompleteProfileInformation.Result.ProfileValidationFailed
import com.drinkit.user.CompleteProfileInformation.Result.Success
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
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class CompleteProfileInformationCommand(
    val author: Author.Connected,
    val profileInformation: ProfileInformation,
)

@Service
@Usecase @ImperativeShell
class CompleteProfileInformation(
    private val userEvents: UserEvents,
    private val users: Users,
    private val clock: Clock,
) {
    sealed interface Result {
        data class Success(val user: User) : Result
        data class ProfileValidationFailed(val errors: List<String>) : Result
        object UserNotFound : Result
        object Forbidden : Result
    }

    fun invoke(userId: UserId, command: CompleteProfileInformationCommand): Result {
        val decision = userEvents.findAllBy(userId)
            ?.let {
                ProfileCompletionDecider.invoke(
                    date = OffsetDateTime.now(clock),
                    decision = UserDecision.from(it),
                    command = command
                )
            }
            ?: return Result.UserNotFound

        return when (decision) {
            is EventToPersist -> Success(userEvents.save(decision.event))
            is ValidationFailed -> ProfileValidationFailed(decision.errors)
            AlreadyCompleted -> Success(users.findBy(userId)!!)
            Unauthorized -> Forbidden
        }
    }
}

@Aggregate
@FunctionalCore
internal object ProfileCompletionDecider {

    sealed interface Decision {
        data class EventToPersist(val event: ProfileCompleted) : Decision
        data class ValidationFailed(val errors: List<String>) : Decision
        object AlreadyCompleted : Decision
        object Unauthorized : Decision
    }

    fun invoke(
        date: OffsetDateTime,
        decision: UserDecision,
        command: CompleteProfileInformationCommand,
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
                profile = command.profileInformation
            )
        )
    }
}