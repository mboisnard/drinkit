package com.drinkit.user

import com.drinkit.user.SendVerificationToken.Result.Success
import com.drinkit.user.SendVerificationToken.Result.UserNotFound
import com.drinkit.user.UserFixtures.Companion.VALID_EMAIL
import com.drinkit.user.VerificationTokenSendingDecider.Decision.CanSendToken
import com.drinkit.user.VerificationTokenSendingDecider.Decision.AlreadyVerified
import com.drinkit.user.VerificationTokenSendingDecider.Decision.Unauthorized
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SendVerificationTokenTest {

    private val userFixtures = UserFixtures()
    private val sendVerificationToken = userFixtures.sendVerificationToken
    private val verificationTokens = userFixtures.verificationTokens
    private val spyMessageSender = userFixtures.spyMessageSender

    @Test
    fun `save verification token for a new registered user and send a message`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val command = SendVerificationTokenCommand(
            author = user.id.toConnectedAuthor(),
            locale = Locale.FRANCE,
        )

        // When
        val result = sendVerificationToken.invoke(user.id, command)

        // Then
        val token = result.shouldBeInstanceOf<Success>().token
        token should {
            it.userId shouldBe user.id
        }
        verificationTokens.findBy(user.id, token.value).shouldNotBeNull()
        spyMessageSender.count() shouldBe 1
    }

    @Test
    fun `can't send verification token to unknown user`() {
        // Given
        val unknownUserId = userFixtures.givenAUserId
        val command = SendVerificationTokenCommand(
            author = userFixtures.givenAConnectedAuthor,
            locale = Locale.FRANCE,
        )

        // When
        val result = sendVerificationToken.invoke(unknownUserId, command)

        // Then
        result.shouldBeInstanceOf<UserNotFound>()
    }

    @Nested
    inner class VerificationTokenSendingDeciderTest {

        @Test
        fun `unauthorized when author is not the affected user`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = SendVerificationTokenCommand(
                author = userFixtures.givenAConnectedAuthor,
                locale = Locale.FRANCE,
            )

            // When
            val decision = VerificationTokenSendingDecider.decide(
                decision = userDecision,
                command = command,
            )

            // Then
            decision.shouldBeInstanceOf<Unauthorized>()
        }

        @Test
        fun `already verified when action was already performed on the user`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = SendVerificationTokenCommand(
                author = userId.toConnectedAuthor(),
                locale = Locale.FRANCE,
            )
            val userDecision = userFixtures.givenAUserHistory(userId)
                .withVerified( ConfirmVerificationTokenCommand(
                    author = userId.toConnectedAuthor(),
                    token = "token",
                ))
                .toUserDecision()

            // When
            val decision = VerificationTokenSendingDecider.decide(
                decision = userDecision,
                command = command,
            )

            // Then
            decision.shouldBeInstanceOf<AlreadyVerified>()
        }

        @Test
        fun `can send token when user is not verified and author is the user`() {
            // Given
            val userId = userFixtures.givenAUserId
            val email = VALID_EMAIL
            val command = SendVerificationTokenCommand(
                author = userId.toConnectedAuthor(),
                locale = Locale.FRANCE,
            )
            val userDecision = userFixtures.givenAUserHistory(userId = userId, email = email)
                .toUserDecision()

            // When
            val decision = VerificationTokenSendingDecider.decide(
                decision = userDecision,
                command = command,
            )

            // Then
            decision.shouldBeInstanceOf<CanSendToken>() should {
                it.userId shouldBe userId
                it.email shouldBe email
            }
        }
    }
}