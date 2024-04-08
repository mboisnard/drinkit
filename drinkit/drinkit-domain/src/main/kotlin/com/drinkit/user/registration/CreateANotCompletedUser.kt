package com.drinkit.user.registration

import com.drinkit.common.IdGenerator
import com.drinkit.messaging.Event
import com.drinkit.messaging.EventPublisher
import com.drinkit.user.Email
import com.drinkit.user.EncodedPassword
import com.drinkit.user.NotCompletedUser
import com.drinkit.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

data class CreateUserCommand(
    val email: Email,
    val password: EncodedPassword,
    val locale: Locale,
)

data class UserCreated(
    val userId: UserId,
    val locale: Locale,
) : Event<UserCreated>

@Service
class CreateANotCompletedUser(
    private val generator: IdGenerator,
    private val notCompletedUsers: NotCompletedUsers,
    private val eventPublisher: EventPublisher,
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    operator fun invoke(command: CreateUserCommand): UserId = with(command) {
        logger.debug { "Creating a new user with command $this" }

        require(!notCompletedUsers.emailExists(email)) {
            "A user already exists with email $email"
        }

        val user = NotCompletedUser.create(
            id = UserId.create(generator),
            email = email,
            password = password,
        )

        logger.debug { "Creating user $user" }

        val userId = notCompletedUsers.create(user)
            ?: error("User not created $user")

        eventPublisher.publish(
            UserCreated(
                userId = userId,
                locale = locale
            )
        )

        logger.info { "User $userId created" }

        return userId
    }
}
