package com.drinkit.user

import com.drinkit.common.Constants.DELETED_ACCOUNT_RETENTION
import com.drinkit.user.ReactivateUser.Result.Reactivated
import com.drinkit.user.UserFixtures.Companion.VALID_PROFILE_INFORMATION
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_ADMIN
import com.drinkit.user.core.Roles.Role.ROLE_USER
import com.drinkit.user.core.UserStatus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

internal class ReactivateUserTest {

    private val userFixtures = UserFixtures()
    private val reactivateUser = userFixtures.reactivateUser
    private val users = userFixtures.users

    init {
        userFixtures.controlledClock.fix()
    }

    @Test
    fun `brings back a user deleted within the retention period, with its roles and its profile`() {
        // Given
        val user = givenADeletedUser(deletedAgo = DELETED_ACCOUNT_RETENTION.minusMinutes(1)) {
            it.withProfileInformationCompleted(
                CompleteProfileInformationCommand(it.authorOf(), VALID_PROFILE_INFORMATION),
            )
                .withPromotedAsAdmin(PromoteAsAdminCommand(it.authorOf()))
        }

        // When
        val result = reactivateUser.invoke(user, ReactivateUserCommand(user.toConnectedAuthor()))

        // Then
        result.shouldBeInstanceOf<Reactivated>().user should {
            it.status shouldBe UserStatus.ACTIVE
            it.roles shouldBe Roles(setOf(ROLE_USER, ROLE_ADMIN))
            it.profile shouldBe VALID_PROFILE_INFORMATION
        }
        users.findEnabledBy(user).shouldNotBeNull()
    }

    @Test
    fun `brings a user who never completed the profile back to the profile completion`() {
        // Given
        val user = givenADeletedUser(deletedAgo = Duration.ofDays(1))

        // When
        val result = reactivateUser.invoke(user, ReactivateUserCommand(user.toConnectedAuthor()))

        // Then
        result.shouldBeInstanceOf<Reactivated>().user.status shouldBe UserStatus.PROFILE_COMPLETION_REQUIRED
    }

    @Nested
    inner class ReactivationDeciderTest {

        private val deletedAt = userFixtures.givenANewDate

        private fun aDeletedUserDecision() = userFixtures.givenAUserHistory()
            .let { it.withDeleted(DeleteUserCommand(it.authorOf()), date = deletedAt) }
            .toUserDecision()

        @Test
        fun `reactivates the deleted user one minute before the end of the retention period`() {
            // Given
            val decision = aDeletedUserDecision()
            val date = deletedAt + DELETED_ACCOUNT_RETENTION - Duration.ofMinutes(1)

            // When
            val result = ReactivationDecider.decide(
                decision,
                ReactivateUserCommand(decision.id.toConnectedAuthor()),
                date,
            )

            // Then
            result.shouldBeInstanceOf<ReactivationDecider.Decision.EventToPersist>().event should {
                it.userId shouldBe decision.id
                it.date shouldBe date
                it.author shouldBe decision.id.toConnectedAuthor()
                it.sequenceId shouldBe decision.nextSequenceId
            }
        }

        @Test
        fun `refuses at the end of the retention period exactly`() {
            // Given
            val decision = aDeletedUserDecision()

            // When
            val result = ReactivationDecider.decide(
                decision,
                ReactivateUserCommand(decision.id.toConnectedAuthor()),
                deletedAt + DELETED_ACCOUNT_RETENTION,
            )

            // Then
            result.shouldBeInstanceOf<ReactivationDecider.Decision.RetentionOver>()
        }

        @Test
        fun `refuses an author who is not the user`() {
            // Given
            val decision = aDeletedUserDecision()

            // When
            val result = ReactivationDecider.decide(
                decision,
                ReactivateUserCommand(userFixtures.givenAConnectedAuthor),
                deletedAt,
            )

            // Then
            result.shouldBeInstanceOf<ReactivationDecider.Decision.Unauthorized>()
        }

        @Test
        fun `has nothing to do for a user that is not deleted`() {
            // Given
            val decision = userFixtures.givenAUserHistory().toUserDecision()

            // When
            val result = ReactivationDecider.decide(
                decision,
                ReactivateUserCommand(decision.id.toConnectedAuthor()),
                deletedAt,
            )

            // Then
            result.shouldBeInstanceOf<ReactivationDecider.Decision.NotDeleted>()
        }
    }

    private fun givenADeletedUser(
        deletedAgo: Duration,
        before: (com.drinkit.user.core.UserHistory) -> com.drinkit.user.core.UserHistory = { it },
    ) = userFixtures.givenAnInitializedUser { history ->
        before(history).withDeleted(
            DeleteUserCommand(history.authorOf()),
            date = userFixtures.givenANewDate.minus(deletedAgo),
        )
    }.id

    private fun com.drinkit.user.core.UserHistory.authorOf() = initEvent.userId.toConnectedAuthor()
}
