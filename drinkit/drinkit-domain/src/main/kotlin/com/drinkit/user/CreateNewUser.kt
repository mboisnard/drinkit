package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.GenerateId
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.user.CreateNewUser.Result.UserCreated
import com.drinkit.user.CreateNewUser.Result.UserAlreadyExists
import com.drinkit.user.core.Email
import com.drinkit.user.core.Roles
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserInitialized
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Command
data class CreateNewUserCommand(
    val email: Email,
    val password: EncodedPassword,
    val author: Author,
)

@Service
@Usecase
class CreateNewUser(
    private val userEvents: UserEvents,
    private val users: Users,
    private val generateId: GenerateId,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    sealed interface Result {
        data class UserCreated(val userId: UserId) : Result
        object UserAlreadyExists : Result
    }

    fun invoke(command: CreateNewUserCommand): Result {
        logger.debug { "Creating new user with command $command" }

        if (users.exists(command.email)) {
            return UserAlreadyExists
        }

        val event = UserInitialized(
            userId = generateId.invoke(UserId::class),
            sequenceId = SequenceId(),
            date = OffsetDateTime.now(clock),
            author = command.author,
            email = command.email,
            password = command.password,
            roles = Roles(setOf(Roles.Role.ROLE_REGISTRATION_IN_PROGRESS))
        )

        val user = userEvents.save(event)

        return UserCreated(user.id)
    }
}