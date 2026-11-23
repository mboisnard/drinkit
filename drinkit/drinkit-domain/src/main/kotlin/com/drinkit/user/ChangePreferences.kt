package com.drinkit.user

import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.event.sourcing.transaction.RetryableTransactional
import com.drinkit.user.ChangePreferences.Result.Forbidden
import com.drinkit.user.ChangePreferences.Result.Success
import com.drinkit.user.ChangePreferences.Result.UserNotFound
import com.drinkit.user.PreferencesChangeDecider.Decision.EventToPersist
import com.drinkit.user.PreferencesChangeDecider.Decision.Unauthorized
import com.drinkit.user.PreferencesChangeDecider.Decision.Unchanged
import com.drinkit.user.core.Author
import com.drinkit.user.core.PreferenceChange
import com.drinkit.user.core.PreferencesChanged
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.UserEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class ChangePreferencesCommand(val author: Author.Connected, val change: PreferenceChange)

@Service
@RetryableTransactional
@Usecase
@ImperativeShell
class ChangePreferences(private val userEvents: UserEvents, private val clock: Clock) {
    sealed interface Result {
        object Success : Result
        object UserNotFound : Result
        object Forbidden : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(userId: UserId, command: ChangePreferencesCommand): Result {
        logger.debug { "Changing the preferences of user $userId with command $command" }

        val decision = userEvents.findAllBy(userId)
            ?.let {
                PreferencesChangeDecider.decide(
                    decision = UserDecision.from(it),
                    command = command,
                    date = OffsetDateTime.now(clock),
                )
            }
            ?: return UserNotFound

        return when (decision) {
            is EventToPersist -> {
                userEvents.save(decision.event)
                Success
            }

            Unchanged -> Success

            Unauthorized -> Forbidden
        }
    }
}

@Aggregate @FunctionalCore
internal object PreferencesChangeDecider {

    sealed interface Decision {
        data class EventToPersist(val event: PreferencesChanged) : Decision
        object Unchanged : Decision
        object Unauthorized : Decision
    }

    fun decide(decision: UserDecision, command: ChangePreferencesCommand, date: OffsetDateTime): Decision {
        val preferences = command.change.applyTo(decision.preferences)

        return when {
            !decision.canEdit(command.author) -> Unauthorized

            preferences == decision.preferences -> Unchanged

            else -> EventToPersist(
                PreferencesChanged(
                    userId = decision.id,
                    date = date,
                    author = command.author,
                    sequenceId = decision.nextSequenceId,
                    preferences = preferences,
                ),
            )
        }
    }
}
