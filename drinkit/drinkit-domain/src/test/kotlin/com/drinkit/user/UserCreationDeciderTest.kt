package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.CorrelationId
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.user.UserCreationDecider.Decision.EmailAlreadyExists
import com.drinkit.user.UserCreationDecider.Decision.EventToPersist
import com.drinkit.user.UserFixtures.Companion.VALID_EMAIL
import com.drinkit.user.UserFixtures.Companion.VALID_PASSWORD
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_REGISTRATION_IN_PROGRESS
import com.drinkit.user.core.UserId
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Locale

internal class UserCreationDeciderTest {

    private val userFixtures = UserFixtures()
    private val generateId = userFixtures.generateId

    @Test
    fun `should return EmailAlreadyExists when email is found in database`() {
        // Given
        val command = CreateNewUserCommand(
            email = VALID_EMAIL,
            password = VALID_PASSWORD,
            author = Author.Unlogged(CorrelationId.create()),
            locale = Locale.FRANCE,
        )

        // When
        val decision = UserCreationDecider.invoke(
            emailFoundInDatabase = true,
            newUserId = generateId.invoke(UserId::class),
            date = OffsetDateTime.now(),
            command = command
        )

        // Then
        decision.shouldBeInstanceOf<EmailAlreadyExists>()
    }

    @Test
    fun `should return UserInitialized event when the given email is not found in database`() {
        // Given
        val emailFoundInDatabase = false
        val newUserId = generateId.invoke(UserId::class)
        val date = OffsetDateTime.now()
        val command = CreateNewUserCommand(
            email = VALID_EMAIL,
            password = VALID_PASSWORD,
            author = Author.Unlogged(CorrelationId.create()),
            locale = Locale.FRANCE,
        )

        // When
        val decision = UserCreationDecider.invoke(
            emailFoundInDatabase = emailFoundInDatabase,
            newUserId = newUserId,
            date = date,
            command = command
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