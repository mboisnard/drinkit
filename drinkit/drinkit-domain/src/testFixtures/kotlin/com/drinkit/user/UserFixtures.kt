package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.Author.Unlogged
import com.drinkit.common.ControlledClock
import com.drinkit.common.CorrelationId
import com.drinkit.common.MockGenerateId
import com.drinkit.common.SpyMessageSender
import com.drinkit.faker
import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.Password
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.Initialized
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.UserHistory
import com.drinkit.user.spi.InMemoryVerificationTokens
import com.drinkit.user.spi.InMemoryUserEventsStore
import com.drinkit.user.spi.InMemoryUsersRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

class UserFixtures(
    val generateId: MockGenerateId = MockGenerateId(),
    val controlledClock: ControlledClock = ControlledClock(),
    val spyEventPublisher: SpyPlatformEventPublisher = SpyPlatformEventPublisher(),
    val spyMessageSender: SpyMessageSender = SpyMessageSender(),
) {
    val users = InMemoryUsersRepository()
    val userEvents = InMemoryUserEventsStore(users)
    val verificationTokens = InMemoryVerificationTokens()

    val createNewUser = CreateNewUser(
        userEvents = userEvents,
        users = users,
        generateId = generateId,
        clock = controlledClock,
        platformEventPublisher = spyEventPublisher,
    )

    val generateVerificationToken = GenerateVerificationToken(
        clock = controlledClock,
    )
    val sendVerificationToken = SendVerificationToken(
        userEvents = userEvents,
        generateVerificationToken = generateVerificationToken,
        verificationTokens = verificationTokens,
        messageSender = spyMessageSender,
    )

    val confirmVerificationToken = ConfirmVerificationToken(
        userEvents = userEvents,
        users = users,
        verificationTokens = verificationTokens,
        clock = controlledClock,
    )

    val completeProfileInformation = CompleteProfileInformation(
        userEvents = userEvents,
        users = users,
        clock = controlledClock,
    )

    val promoteAsAdmin = PromoteAsAdmin(
        userEvents = userEvents,
        users = users,
        clock = controlledClock,
    )

    val deleteUser = DeleteUser(
        userEvents = userEvents,
        clock = controlledClock,
    )

    val givenAUserId: UserId get() = generateId.invoke(UserId::class)
    val givenANewDate: OffsetDateTime get() = OffsetDateTime.now(controlledClock).truncatedTo(ChronoUnit.MICROS)

    val givenAnUnloggedAuthor: Unlogged get() = Unlogged(CorrelationId.create())
    val givenAConnectedAuthor: Author.Connected get() = Author.Connected(givenAUserId)

    fun givenAnInitializedUser(
        userId: UserId = givenAUserId,
        email: Email = VALID_EMAIL,
        historyModifierFunction: (history: UserHistory) -> UserHistory = { it },
    ): User {
        val history = givenAUserHistory(userId = userId, email = email)
            .let { history -> historyModifierFunction(history) }

        val allEvents = listOf(history.initEvent) + history.remainingEvents
        return allEvents.map { userEvents.save(it) }.last()
    }

    fun givenAUserHistory(
        userId: UserId = givenAUserId,
        email: Email = VALID_EMAIL,
        author: Author = Unlogged(CorrelationId.create()),
        initEventModifier: (Initialized) -> Initialized = { it },
    ): UserHistory {
        val decision = UserCreationDecider.invoke(
            command = CreateNewUserCommand(
                author = author,
                email = email,
                password = VALID_PASSWORD,
                locale = Locale.FRANCE,
            ),
            emailFoundInDatabase = false,
            newUserId = userId,
            date = givenANewDate,
        )

        return when (decision) {
            is UserCreationDecider.Decision.EventToPersist -> UserHistory(initEventModifier(decision.event))
            else -> throw IllegalArgumentException("Wrong user creation state here")
        }
    }

    companion object {
        val VALID_EMAIL = faker.randomProvider.randomClassInstance<Email> {
            typeGenerator<String> { faker.internet.safeEmail() }
        }
        val VALID_PASSWORD = EncodedPassword.from(Password("F@kePa\$\$w0rD")) { it }
        val VALID_PROFILE_INFORMATION = ProfileInformation(
            firstName = faker.randomProvider.randomClassInstance {
                typeGenerator<String> { faker.name.firstName() }
            },
            lastName = faker.randomProvider.randomClassInstance {
                typeGenerator<String> { faker.name.lastName() }
            },
            birthDate = faker.randomProvider.randomClassInstance {
                typeGenerator<LocalDate> { faker.person.birthDate(faker.random.nextLong(25)) }
            }
        )
    }
}