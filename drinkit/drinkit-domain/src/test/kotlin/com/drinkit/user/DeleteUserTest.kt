package com.drinkit.user

import com.drinkit.user.DeleteUser.Result.Success
import com.drinkit.user.DeleteUser.Result.UserNotFound
import com.drinkit.user.UserDeletionDecider.Decision.AlreadyDeleted
import com.drinkit.user.UserDeletionDecider.Decision.EventToPersist
import com.drinkit.user.UserDeletionDecider.Decision.Unauthorized
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DeleteUserTest {

    private val userFixtures = UserFixtures()
    private val deleteUser = userFixtures.deleteUser
    private val userEvents = userFixtures.userEvents

    @Test
    fun `delete an existing user`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val command = DeleteUserCommand(
            author = user.id.toConnectedAuthor(),
        )

        // When
        val result = deleteUser.invoke(user.id, command)

        // Then
        result.shouldBeInstanceOf<Success>()
        userEvents.findAllBy(user.id).shouldNotBeNull() should {
            it.remainingEvents shouldHaveSize 1
        }
    }

    @Test
    fun `can't delete an unknown user`() {
        // Given
        val unknownUserId = userFixtures.givenAUserId
        val command = DeleteUserCommand(
            author = userFixtures.givenAConnectedAuthor,
        )

        // When
        val result = deleteUser.invoke(unknownUserId, command)

        // Then
        result.shouldBeInstanceOf<UserNotFound>()
    }

    @Nested
    inner class UserDeletionDeciderTest {

        @Test
        fun `unauthorized when author is not the affected user`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = DeleteUserCommand(
                author = userFixtures.givenAConnectedAuthor
            )

            // When
            val decision = UserDeletionDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<Unauthorized>()
        }

        @Test
        fun `already deleted when action was already performed on the user`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = DeleteUserCommand(
                author = userId.toConnectedAuthor()
            )
            val userDecision = userFixtures.givenAUserHistory(userId = userId)
                .withDeleted(command)
                .toUserDecision()

            // When
            val decision = UserDeletionDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate
            )

            // Then
            decision.shouldBeInstanceOf<AlreadyDeleted>()
        }

        @Test
        fun `event to persist when deletion is valid`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = DeleteUserCommand(
                author = userId.toConnectedAuthor(),
            )
            val userDecision = userFixtures.givenAUserHistory(userId)
                .toUserDecision()

            // When
            val decision = UserDeletionDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate
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