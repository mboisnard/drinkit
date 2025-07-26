package com.drinkit.user

import com.drinkit.event.sourcing.SequenceId
import com.drinkit.user.CreateNewUser.Result.UserAlreadyExists
import com.drinkit.user.CreateNewUser.Result.UserCreated
import com.drinkit.user.UserCreationDecider.Decision.EmailAlreadyExists
import com.drinkit.user.UserCreationDecider.Decision.EventToPersist
import com.drinkit.user.UserCreationDecider.Decision.ValidationFailed
import com.drinkit.user.UserFixtures.Companion.VALID_EMAIL
import com.drinkit.user.UserFixtures.Companion.VALID_PASSWORD
import com.drinkit.user.core.Email
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_REGISTRATION_IN_PROGRESS
import com.drinkit.user.core.UserId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

internal class CreateNewUserTest {

    private val userFixtures = UserFixtures()
    private val createNewUser = userFixtures.createNewUser
    private val generateId = userFixtures.generateId
    private val users = userFixtures.users
    private val userEvents = userFixtures.userEvents
    private val spyEventPublisher = userFixtures.spyEventPublisher

    @Test
    fun `persist a new user with event & projection and publish a platform event`() {
        // Given
        val command = CreateNewUserCommand(
            email = VALID_EMAIL,
            password = VALID_PASSWORD,
            author = userFixtures.givenAnUnloggedAuthor,
            locale = Locale.FRANCE,
        )

        // When
        val result = createNewUser.invoke(command)

        // Then
        result.shouldBeInstanceOf<UserCreated>()
        val createdUser = result.user
        userEvents.findAllBy(createdUser.id).shouldNotBeNull()
        users.findBy(createdUser.id) shouldBe createdUser
        spyEventPublisher.countEventsOfType(com.drinkit.user.spi.UserCreated::class) shouldBe 1
    }

    @Test
    fun `can't create the user if his email is already used`() {
        // Given
        val email = VALID_EMAIL
        val command = CreateNewUserCommand(
            email = email,
            password = VALID_PASSWORD,
            author = userFixtures.givenAnUnloggedAuthor,
            locale = Locale.FRANCE,
        )
        userFixtures.givenAnInitializedUser(email = email)
        users.count() shouldBe 1

        // When
        val result = createNewUser.invoke(command)

        // Then
        result.shouldBeInstanceOf<UserAlreadyExists>()
        users.count() shouldBe 1
    }

    @Nested
    inner class UserCreationDeciderTest {

        @Test
        fun `new user email can already exists in database`() {
            // Given
            val command = CreateNewUserCommand(
                email = VALID_EMAIL,
                password = VALID_PASSWORD,
                author = userFixtures.givenAnUnloggedAuthor,
                locale = Locale.FRANCE,
            )

            // When
            val decision = UserCreationDecider.invoke(
                command = command,
                emailFoundInDatabase = true,
                newUserId = generateId.invoke(UserId::class),
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<EmailAlreadyExists>()
        }

        @Test
        fun `new user email should be valid`() {
            // Given
            val command = CreateNewUserCommand(
                email = Email("invalidEmail@"),
                password = VALID_PASSWORD,
                author = userFixtures.givenAnUnloggedAuthor,
                locale = Locale.FRANCE,
            )

            // When
            val decision = UserCreationDecider.invoke(
                command = command,
                emailFoundInDatabase = false,
                newUserId = generateId.invoke(UserId::class),
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<ValidationFailed>()
        }

        @Test
        fun `initialized user will start with registration_in_progress role`() {
            // Given
            val newUserId = generateId.invoke(UserId::class)
            val date = userFixtures.givenANewDate
            val command = CreateNewUserCommand(
                email = VALID_EMAIL,
                password = VALID_PASSWORD,
                author = userFixtures.givenAnUnloggedAuthor,
                locale = Locale.FRANCE,
            )

            // When
            val decision = UserCreationDecider.invoke(
                command = command,
                emailFoundInDatabase = false,
                newUserId = newUserId,
                date = date,
            )

            // Then
            decision.shouldBeInstanceOf<EventToPersist>().event should {
                it.userId shouldBe newUserId
                it.sequenceId shouldBe SequenceId()
                it.date shouldBe date
                it.author shouldBe command.author
                it.email shouldBe command.email
                it.password shouldBe command.password
                it.roles shouldBe Roles(setOf(ROLE_REGISTRATION_IN_PROGRESS))
            }
        }
    }
}