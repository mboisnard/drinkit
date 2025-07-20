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
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_REGISTRATION_IN_PROGRESS
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserInitialized
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.Users
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Locale

@Command
data class CreateNewUserCommand(
    val email: Email,
    val password: EncodedPassword,
    val locale: Locale,
    val author: Author,
)

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

    fun invoke(command: CreateNewUserCommand): Result {

        val decision = UserCreationDecider.invoke(
            emailFoundInDatabase = users.exists(command.email),
            newUserId = generateId.invoke(UserId::class),
            date = OffsetDateTime.now(clock),
            command = command
        )

        return when (decision) {
            is EmailAlreadyExists -> UserAlreadyExists
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
        }
    }
}

@FunctionalCore
internal object UserCreationDecider {

    sealed interface Decision {
        object EmailAlreadyExists : Decision
        data class EventToPersist(val event: UserInitialized) : Decision
    }

    fun invoke(
        emailFoundInDatabase: Boolean,
        newUserId: UserId,
        date: OffsetDateTime,
        command: CreateNewUserCommand,
    ): Decision {
        if (emailFoundInDatabase) {
            return EmailAlreadyExists
        }

        return EventToPersist(
            UserInitialized(
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