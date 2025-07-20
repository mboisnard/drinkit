package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.Author.Unlogged
import com.drinkit.common.ControlledClock
import com.drinkit.common.CorrelationId
import com.drinkit.common.MockGenerateId
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.faker
import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.Password
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.Roles
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserInitialized
import com.drinkit.user.core.UserStatus
import com.drinkit.user.registration.NotCompletedUser
import com.drinkit.user.spi.InMemoryUserEventsStore
import com.drinkit.user.spi.InMemoryUsersRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Locale

class UserFixtures(
    val generateId: MockGenerateId = MockGenerateId(),
    val controlledClock: ControlledClock = ControlledClock(),
    val spyEventPublisher: SpyPlatformEventPublisher = SpyPlatformEventPublisher(),
) {

    val users = InMemoryUsersRepository()
    val userEvents = InMemoryUserEventsStore(users)

    val createNewUser = CreateNewUser(
        userEvents = userEvents,
        users = users,
        generateId = generateId,
        clock = controlledClock,
        platformEventPublisher = spyEventPublisher,
    )

    fun givenAUserInitializedEvent(
        id: UserId = generateId.invoke(UserId::class),
        author: Author = Unlogged(CorrelationId.create())
    ) = UserInitialized(
        userId = id,
        sequenceId = SequenceId(),
        date = OffsetDateTime.now(controlledClock),
        author = author,
        email = VALID_EMAIL,
        password = VALID_PASSWORD,
        roles = Roles(setOf(Roles.Role.ROLE_REGISTRATION_IN_PROGRESS)),
        preferredLocale = Locale.FRANCE,
    )

    fun givenAUser(
        id: UserId = generateId.invoke(UserId::class),
        roles: Roles = Roles(setOf(Roles.Role.ROLE_USER)),
    ) = User(
            id = id,
            email = faker.randomProvider.randomClassInstance {
                typeGenerator<String> { faker.internet.safeEmail() }
            },
            password = VALID_PASSWORD,
            roles = roles,
            lastConnection = null,
            profile = ProfileInformation(
                firstName = faker.randomProvider.randomClassInstance {
                    typeGenerator<String> { faker.name.firstName() }
                },
                lastName = faker.randomProvider.randomClassInstance {
                    typeGenerator<String> { faker.name.lastName() }
                },
                birthDate = faker.randomProvider.randomClassInstance {
                    typeGenerator<LocalDate> { faker.person.birthDate(faker.random.nextLong(25)) }
                },
            ),
            status = UserStatus.ACTIVE,
        )

    companion object {
        val VALID_EMAIL = faker.randomProvider.randomClassInstance<Email> {
            typeGenerator<String> { faker.internet.safeEmail() }
        }
        val VALID_PASSWORD = EncodedPassword.from(Password("F@kePa\$\$w0rD")) { it }

        fun givenANotCompletedUser(
            id: UserId? = null,
            email: Email? = null,
            status: String = "USER_CREATED"
        ): NotCompletedUser =
            NotCompletedUser(
                id = id ?: faker.randomProvider.randomClassInstance {
                    typeGenerator<String> { "659ee3164b1d53340c4f7608" }
                },
                email = email ?: faker.randomProvider.randomClassInstance {
                    typeGenerator<String> { faker.internet.safeEmail() }
                },
                password = EncodedPassword.from(Password("F@kePa$\$w0rD"), encoder = { it }),
                firstName = faker.randomProvider.randomClassInstance {
                    typeGenerator<String> { faker.name.firstName() }
                },
                lastName = faker.randomProvider.randomClassInstance {
                    typeGenerator<String> { faker.name.lastName() }
                },
                birthDate = faker.randomProvider.randomClassInstance {
                    typeGenerator<LocalDate> { faker.person.birthDate(faker.random.nextLong(25)) }
                },
                status = status,
                completed = false,
                enabled = true,
            )
    }
}
