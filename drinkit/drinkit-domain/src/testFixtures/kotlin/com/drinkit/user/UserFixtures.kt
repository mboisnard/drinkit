package com.drinkit.user

import com.drinkit.faker
import java.time.LocalDate

object UserFixtures {

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
            firstname = faker.randomProvider.randomClassInstance {
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