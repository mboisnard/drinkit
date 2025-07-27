package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.event.sourcing.transaction.RetryableTransactional
import com.drinkit.user.PromoteAsAdmin.Result.Success
import com.drinkit.user.PromoteAsAdmin.Result.UserNotFound
import com.drinkit.user.AdminPromotionDecider.Decision.AlreadyAdmin
import com.drinkit.user.AdminPromotionDecider.Decision.EventToPersist
import com.drinkit.user.AdminPromotionDecider.Decision.Unauthorized
import com.drinkit.user.core.PromotedAsAdmin
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
data class PromoteAsAdminCommand(
    val author: Author.Connected
)

@Service
@RetryableTransactional
@Usecase @ImperativeShell
class PromoteAsAdmin(
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

    fun invoke(userId: UserId, command: PromoteAsAdminCommand): Result {
        logger.debug { "Admin promotion for user $userId with command $command" }

        val decision = userEvents.findAllBy(userId)
            ?.let {
                AdminPromotionDecider.decide(
                    decision = UserDecision.from(it),
                    command = command,
                    date = OffsetDateTime.now(clock),
                )
            }
            ?: return UserNotFound

        return when (decision) {
            is EventToPersist -> {
                logger.info { "User $userId has been promoted as admin by ${command.author}" }
                Success(userEvents.save(decision.event))
            }
            AlreadyAdmin -> Success(users.findEnabledBy(userId)!!)
            Unauthorized -> Result.Forbidden
        }
    }
}

@Aggregate @FunctionalCore
internal object AdminPromotionDecider {

    sealed interface Decision {
        data class EventToPersist(val event: PromotedAsAdmin) : Decision
        object AlreadyAdmin : Decision
        object Unauthorized : Decision
    }

    fun decide(
        decision: UserDecision,
        command: PromoteAsAdminCommand,
        date: OffsetDateTime
    ): Decision {
        // TODO Unauthorized if author is not admin or system

        if (!decision.isProfileCompleted) {
            return Unauthorized
        }

        if (decision.isAdmin) {
            return AlreadyAdmin
        }

        return EventToPersist(
            PromotedAsAdmin(
                userId = decision.id,
                date = date,
                author = command.author,
                sequenceId = decision.nextSequenceId,
            )
        )
    }
}