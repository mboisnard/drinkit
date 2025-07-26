package com.drinkit.user

import com.drinkit.user.ConfirmVerificationToken.Result.NotFound
import com.drinkit.user.ConfirmVerificationToken.Result.Success
import com.drinkit.user.VerificationTokenDecider.Decision.AlreadyVerified
import com.drinkit.user.VerificationTokenDecider.Decision.EventToPersist
import com.drinkit.user.VerificationTokenDecider.Decision.Expired
import com.drinkit.user.VerificationTokenDecider.Decision.Unauthorized
import com.drinkit.user.core.VerificationToken
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

internal class ConfirmVerificationTokenTest {

    private val userFixtures = UserFixtures()
    private val sendVerificationToken = userFixtures.sendVerificationToken
    private val confirmVerificationToken = userFixtures.confirmVerificationToken
    private val userEvents = userFixtures.userEvents
    private val verificationTokens = userFixtures.verificationTokens

    @Test
    fun `confirm verification token for an existing user and mark the user as verified`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val verificationSent = sendVerificationToken.invoke(
            userId = user.id,
            command = SendVerificationTokenCommand(user.id.toConnectedAuthor(), Locale.FRANCE),
        )
        verificationSent.shouldBeInstanceOf<SendVerificationToken.Result.Success>()

        val command = ConfirmVerificationTokenCommand(
            author = user.id.toConnectedAuthor(),
            token = verificationSent.token.value,
        )

        // When
        val result = confirmVerificationToken.invoke(user.id, command)

        // Then
        result.shouldBeInstanceOf<Success>().user should {
            it.verified shouldBe true
        }
        userEvents.findAllBy(user.id).shouldNotBeNull() should {
            it.remainingEvents shouldHaveSize 1
        }
    }

    @Test
    fun `can't confirm verification token when token is not found`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val command = ConfirmVerificationTokenCommand(
            author = user.id.toConnectedAuthor(),
            token = "non-existent-token",
        )

        // When
        val result = confirmVerificationToken.invoke(user.id, command)

        // Then
        result.shouldBeInstanceOf<NotFound>()
    }

    @Test
    fun `can't confirm verification token for unknown user`() {
        // Given
        val unknownUser = userFixtures.givenAConnectedAuthor
        verificationTokens.createOrUpdate(
            VerificationToken(
                userId = unknownUser.userId,
                value = "verification-token",
                expiryDate = userFixtures.givenANewDate.plusHours(1),
            )
        )
        val command = ConfirmVerificationTokenCommand(
            author = unknownUser,
            token = "verification-token",
        )

        // When
        val result = confirmVerificationToken.invoke(unknownUser.userId, command)

        // Then
        result.shouldBeInstanceOf<NotFound>()
    }

    @Nested
    inner class VerificationTokenDeciderTest {

        @Test
        fun `unauthorized when author is not the affected user`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = ConfirmVerificationTokenCommand(
                author = userFixtures.givenAConnectedAuthor,
                token = "verification-token",
            )
            val date = userFixtures.givenANewDate

            // When
            val decision = VerificationTokenDecider.decide(
                decision = userDecision,
                command = command,
                foundToken = VerificationToken(
                    userId = userDecision.id,
                    value = command.token,
                    expiryDate = date.plusHours(1),
                ),
                date = date,
            )

            // Then
            decision.shouldBeInstanceOf<Unauthorized>()
        }

        @Test
        fun `expired when token expiry date is in the past`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = ConfirmVerificationTokenCommand(
                author = userDecision.id.toConnectedAuthor(),
                token = "verification-token",
            )
            val date = userFixtures.givenANewDate

            // When
            val decision = VerificationTokenDecider.decide(
                decision = userDecision,
                command = command,
                foundToken = VerificationToken(
                    userId = userDecision.id,
                    value = command.token,
                    expiryDate = date.minusHours(1),
                ),
                date = date,
            )

            // Then
            decision.shouldBeInstanceOf<Expired>()
        }

        @Test
        fun `already verified when action was already performed on the user`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = ConfirmVerificationTokenCommand(
                author = userId.toConnectedAuthor(),
                token = "verification-token"
            )
            val date = userFixtures.givenANewDate
            val userDecision = userFixtures.givenAUserHistory(userId = userId)
                .withVerified(command)
                .toUserDecision()

            // When
            val decision = VerificationTokenDecider.decide(
                decision = userDecision,
                command = command,
                foundToken = VerificationToken(
                    userId = userDecision.id,
                    value = command.token,
                    expiryDate = date.plusHours(1)
                ),
                date = date,
            )

            // Then
            decision.shouldBeInstanceOf<AlreadyVerified>()
        }

        @Test
        fun `event to persist when verification is valid`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = ConfirmVerificationTokenCommand(
                author = userDecision.id.toConnectedAuthor(),
                token = "verification-token"
            )
            val date = userFixtures.givenANewDate

            // When
            val decision = VerificationTokenDecider.decide(
                decision = userDecision,
                command = command,
                foundToken = VerificationToken(
                    userId = userDecision.id,
                    value = command.token,
                    expiryDate = date.plusHours(1)
                ),
                date = date
            )

            // Then
            decision.shouldBeInstanceOf<EventToPersist>().event should {
                it.userId shouldBe userDecision.id
                it.author shouldBe command.author
                it.sequenceId shouldBe userDecision.nextSequenceId
            }
        }
    }
}