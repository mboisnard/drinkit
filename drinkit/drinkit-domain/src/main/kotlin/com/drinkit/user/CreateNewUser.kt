package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.GenerateId
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.user.CreateNewUser.Result.UserCreated
import com.drinkit.user.CreateNewUser.Result.UserAlreadyExists
import com.drinkit.user.UserCreationDecider.Decision.EmailAlreadyExists
import com.drinkit.user.UserCreationDecider.Decision.EventToPersist
import com.drinkit.user.UserCreationDecider.Decision.ValidationFailed
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_REGISTRATION_IN_PROGRESS
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.Initialized
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Locale

@Command
data class CreateNewUserCommand(
    val author: Author,
    val email: Email,
    val password: EncodedPassword,
    val locale: Locale,
)

@Transactional // TODO nested Transactional
@Service
@Usecase @ImperativeShell
class CreateNewUser(
    private val userEvents: UserEvents,
    private val users: Users,
    private val generateId: GenerateId,
    private val clock: Clock,
    private val platformEventPublisher: PlatformEventPublisher,
) {
    sealed interface Result {
        data class UserCreated(val user: User) : Result
        object UserAlreadyExists : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(command: CreateNewUserCommand): Result {
        logger.debug { "Creating a new user with command $command" }

        val decision = UserCreationDecider.invoke(
            command = command,
            emailFoundInDatabase = users.exists(command.email),
            newUserId = generateId.invoke(UserId::class),
            date = OffsetDateTime.now(clock),
        )

        return when (decision) {
            is EventToPersist -> {
                val user = userEvents.save(decision.event)
                platformEventPublisher.publish(
                    com.drinkit.user.spi.UserCreated(
                        userId = user.id,
                        locale = command.locale,
                    )
                )

                UserCreated(user)
            }
            is EmailAlreadyExists -> UserAlreadyExists
            is ValidationFailed -> throw IllegalStateException("Validation failed: ${decision.errors}")
        }
    }
}

@FunctionalCore
internal object UserCreationDecider {

    sealed interface Decision {
        data class EventToPersist(val event: Initialized) : Decision
        object EmailAlreadyExists : Decision
        data class ValidationFailed(val errors: List<String>) : Decision
    }

    fun invoke(
        command: CreateNewUserCommand,
        emailFoundInDatabase: Boolean,
        newUserId: UserId,
        date: OffsetDateTime,
    ): Decision {
        if (emailFoundInDatabase) {
            return EmailAlreadyExists
        }

        val validationErrors = command.email.validate()
        if (command.email.validate().isNotEmpty()) {
            return ValidationFailed(validationErrors)
        }

        return EventToPersist(
            Initialized(
                userId = newUserId,
                sequenceId = SequenceId(),
                date = date,
                author = command.author,
                email = command.email,
                password = command.password,
                roles = Roles(setOf(ROLE_REGISTRATION_IN_PROGRESS)),
                preferredLocale = command.locale,
            )
        )
    }
}