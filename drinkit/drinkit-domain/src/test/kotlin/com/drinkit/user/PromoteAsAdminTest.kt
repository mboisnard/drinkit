package com.drinkit.user

import com.drinkit.user.PromoteAsAdmin.Result.Success
import com.drinkit.user.PromoteAsAdmin.Result.UserNotFound
import com.drinkit.user.AdminPromotionDecider.Decision.AlreadyAdmin
import com.drinkit.user.AdminPromotionDecider.Decision.EventToPersist
import com.drinkit.user.AdminPromotionDecider.Decision.Unauthorized
import com.drinkit.user.UserFixtures.Companion.VALID_PROFILE_INFORMATION
import com.drinkit.user.core.Roles.Role.ROLE_ADMIN
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PromoteAsAdminTest {

    private val userFixtures = UserFixtures()
    private val promoteAsAdmin = userFixtures.promoteAsAdmin
    private val userEvents = userFixtures.userEvents

    @Test
    fun `add admin role when promoting a user as admin`() {
        // Given
        val userId = userFixtures.givenAUserId
        val user = userFixtures.givenAnInitializedUser(userId) { history ->
            history.withProfileInformationCompleted(
                CompleteProfileInformationCommand(
                    author = userId.toConnectedAuthor(),
                    profileInformation = VALID_PROFILE_INFORMATION,
                )
            )
        }
        val command = PromoteAsAdminCommand(
            author = user.id.toConnectedAuthor()
        )

        // When
        val result = promoteAsAdmin.invoke(user.id, command)

        // Then
        result.shouldBeInstanceOf<Success>() should {
            it.user.roles.values shouldContain ROLE_ADMIN
        }
        userEvents.findAllBy(user.id).shouldNotBeNull() should {
            it.remainingEvents shouldHaveSize 2
        }
    }

    @Test
    fun `can't promote an unknown user`() {
        // Given
        val unknownUserId = userFixtures.givenAUserId
        val command = PromoteAsAdminCommand(
            author = userFixtures.givenAConnectedAuthor,
        )

        // When
        val result = promoteAsAdmin.invoke(unknownUserId, command)

        // Then
        result.shouldBeInstanceOf<UserNotFound>()
    }

    @Nested
    inner class AdminPromotionDeciderTest {

        @Test
        fun `unauthorized when profile is not completed`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = PromoteAsAdminCommand(
                author = userFixtures.givenAConnectedAuthor,
            )

            // When
            val decision = AdminPromotionDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<Unauthorized>()
        }

        @Test
        fun `already admin when action was already performed on the user`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = PromoteAsAdminCommand(
                author = userId.toConnectedAuthor()
            )
            val userDecision = userFixtures.givenAUserHistory(userId = userId)
                .withProfileInformationCompleted(
                    CompleteProfileInformationCommand(
                        author = userId.toConnectedAuthor(),
                        profileInformation = VALID_PROFILE_INFORMATION,
                    )
                )
                .withPromotedAsAdmin(command)
                .toUserDecision()

            // When
            val decision = AdminPromotionDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<AlreadyAdmin>()
        }

        @Test
        fun `event to persist when promotion is valid`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = PromoteAsAdminCommand(
                author = userId.toConnectedAuthor()
            )
            val userDecision = userFixtures.givenAUserHistory(userId)
                .withProfileInformationCompleted(
                    CompleteProfileInformationCommand(
                        author = userId.toConnectedAuthor(),
                        profileInformation = UserFixtures.VALID_PROFILE_INFORMATION
                    )
                ).toUserDecision()

            // When
            val decision = AdminPromotionDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<EventToPersist>().event should {
                it.userId shouldBe userId
                it.author shouldBe command.author
                it.sequenceId shouldBe userDecision.nextSequenceId
            }
        }
    }
}