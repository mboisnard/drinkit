package com.drinkit.user

import com.drinkit.user.CompleteProfileInformation.Result.Success
import com.drinkit.user.CompleteProfileInformation.Result.UserNotFound
import com.drinkit.user.ProfileCompletionDecider.Decision.AlreadyCompleted
import com.drinkit.user.ProfileCompletionDecider.Decision.Unauthorized
import com.drinkit.user.ProfileCompletionDecider.Decision.ValidationFailed
import com.drinkit.user.UserFixtures.Companion.VALID_PROFILE_INFORMATION
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_USER
import com.drinkit.user.core.UserDecision
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class CompleteProfileInformationTest {

    private val userFixtures = UserFixtures()
    private val completeProfileInformation = userFixtures.completeProfileInformation
    private val userEvents = userFixtures.userEvents

    @Test
    fun `save profile information completion for an existing user and change his role to be able to access more features`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val command = CompleteProfileInformationCommand(
            author = user.id.toConnectedAuthor(),
            profileInformation = VALID_PROFILE_INFORMATION,
        )

        // When
        val result = completeProfileInformation.invoke(user.id, command)

        // Then
        result.shouldBeInstanceOf<Success>() should {
            it.user.profile shouldBe VALID_PROFILE_INFORMATION
            it.user.roles shouldBe Roles(setOf(ROLE_USER))
        }
        userEvents.findAllBy(user.id).shouldNotBeNull() should {
            it.remainingEvents shouldHaveSize 1
        }
    }

    @Test
    fun `can't complete profile information for unknown user`() {
        // Given
        val unknownUserId = userFixtures.givenAUserId
        val command = CompleteProfileInformationCommand(
            author = userFixtures.givenAConnectedAuthor,
            profileInformation = VALID_PROFILE_INFORMATION,
        )

        // When
        val result = completeProfileInformation.invoke(unknownUserId, command)

        // Then
        result.shouldBeInstanceOf<UserNotFound>()
    }

    @Test
    fun `throw exception when there is some validation issues`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val command = CompleteProfileInformationCommand(
            author = user.id.toConnectedAuthor(),
            profileInformation = ProfileInformation(
                firstName = FirstName("a"),
                lastName = LastName("doe"),
                birthDate = null,
            ),
        )

        // When / Then
        assertThrows<IllegalStateException> {
            completeProfileInformation.invoke(user.id, command)
        }
    }

    @Nested
    inner class ProfileCompletionDeciderTest {

        @Test
        fun `unauthorized when author is not the affected user`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .let { UserDecision.from(it) }
            val command = CompleteProfileInformationCommand(
                author = userFixtures.givenAConnectedAuthor,
                profileInformation = VALID_PROFILE_INFORMATION,
            )

            // When
            val decision = ProfileCompletionDecider.invoke(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<Unauthorized>()
        }

        @Test
        fun `profile already completed when action was already performed on the user`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = CompleteProfileInformationCommand(
                author = userId.toConnectedAuthor(),
                profileInformation = VALID_PROFILE_INFORMATION,
            )
            val userDecision = userFixtures.givenAUserHistory(userId = userId)
                .withProfileInformationCompleted(command)
                .toUserDecision()

            // When
            val decision = ProfileCompletionDecider.invoke(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<AlreadyCompleted>()
        }

        @Test
        fun `detect profile information validation issues`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = CompleteProfileInformationCommand(
                author = userDecision.id.toConnectedAuthor(),
                profileInformation = ProfileInformation(
                    firstName = FirstName("a"),
                    lastName = LastName("lastname".repeat(20)),
                    birthDate = BirthDate(LocalDate.of(1805, 1, 1))
                ),
            )

            // When
            val decision = ProfileCompletionDecider.invoke(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<ValidationFailed>()
            decision.errors shouldHaveSize 3
        }
    }
}