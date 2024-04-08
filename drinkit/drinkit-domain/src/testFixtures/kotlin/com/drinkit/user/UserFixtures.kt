package com.drinkit.user

import com.drinkit.common.ControlledClock
import com.drinkit.common.ControlledRandom
import com.drinkit.common.ControlledIdGenerator
import com.drinkit.common.SpyMessageSender
import com.drinkit.faker
import com.drinkit.messaging.SpyEventPublisher
import com.drinkit.user.registration.*
import java.time.LocalDate

class UserFixtures {

    val controlledIdGenerator = ControlledIdGenerator()

    val spyEventPublisher = SpyEventPublisher()

    val controlledClock = ControlledClock()

    val controlledRandom = ControlledRandom.value

    val spyMessageSender = SpyMessageSender()

    val notCompletedUsers = InMemoryNotCompletedUsers()

    val verificationTokens = InMemoryVerificationTokens()

    // Not Completed User Registration Usecases
    val createANotCompletedUser = CreateANotCompletedUser(
        generator = controlledIdGenerator,
        notCompletedUsers = notCompletedUsers,
        eventPublisher = spyEventPublisher,
    )

    val generateVerificationToken = GenerateVerificationToken(
        clock = controlledClock,
        random = controlledRandom,
    )

    val completeUserInformation = CompleteUserInformation(
        notCompletedUsers = notCompletedUsers,
    )

    val validateEmail = ValidateEmail(
        notCompletedUsers = notCompletedUsers,
        generateVerificationToken = generateVerificationToken,
        verificationTokens = verificationTokens,
        messageSender = spyMessageSender,
        clock = controlledClock,
    )

    companion object {
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