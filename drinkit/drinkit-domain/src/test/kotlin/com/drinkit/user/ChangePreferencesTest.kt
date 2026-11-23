package com.drinkit.user

import com.drinkit.user.ChangePreferences.Result.Success
import com.drinkit.user.ChangePreferences.Result.UserNotFound
import com.drinkit.user.PreferencesChangeDecider.Decision.EventToPersist
import com.drinkit.user.PreferencesChangeDecider.Decision.Unauthorized
import com.drinkit.user.PreferencesChangeDecider.Decision.Unchanged
import com.drinkit.user.core.ChooseLanguage
import com.drinkit.user.core.Preferences
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

internal class ChangePreferencesTest {

    private val userFixtures = UserFixtures()
    private val changePreferences = userFixtures.changePreferences
    private val users = userFixtures.users

    @Test
    fun `the user reads their account in the language they chose`() {
        // Given
        val user = userFixtures.givenAnInitializedUser()
        val command = ChangePreferencesCommand(
            author = user.id.toConnectedAuthor(),
            change = ChooseLanguage(Locale.ENGLISH),
        )

        // When
        val result = changePreferences.invoke(user.id, command)

        // Then
        result.shouldBeInstanceOf<Success>()
        users.findEnabledBy(user.id).shouldNotBeNull().preferences shouldBe Preferences(language = Locale.ENGLISH)
    }

    @Test
    fun `can't change the preferences of an unknown user`() {
        // Given
        val command = ChangePreferencesCommand(
            author = userFixtures.givenAConnectedAuthor,
            change = ChooseLanguage(Locale.ENGLISH),
        )

        // When
        val result = changePreferences.invoke(userFixtures.givenAUserId, command)

        // Then
        result.shouldBeInstanceOf<UserNotFound>()
    }

    @Nested
    inner class PreferencesChangeDeciderTest {

        @Test
        fun `unauthorized when author is not the affected user`() {
            // Given
            val userDecision = userFixtures.givenAUserHistory()
                .toUserDecision()
            val command = ChangePreferencesCommand(
                author = userFixtures.givenAConnectedAuthor,
                change = ChooseLanguage(Locale.ENGLISH),
            )

            // When
            val decision = PreferencesChangeDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<Unauthorized>()
        }

        @Test
        fun `unchanged when the preferences already hold the change`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = ChangePreferencesCommand(
                author = userId.toConnectedAuthor(),
                change = ChooseLanguage(Locale.ENGLISH),
            )
            val userDecision = userFixtures.givenAUserHistory(userId)
                .withPreferencesChanged(command)
                .toUserDecision()

            // When
            val decision = PreferencesChangeDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<Unchanged>()
        }

        @Test
        fun `event to persist holds every preference once changed`() {
            // Given
            val userId = userFixtures.givenAUserId
            val command = ChangePreferencesCommand(
                author = userId.toConnectedAuthor(),
                change = ChooseLanguage(Locale.ENGLISH),
            )
            val userDecision = userFixtures.givenAUserHistory(userId)
                .toUserDecision()

            // When
            val decision = PreferencesChangeDecider.decide(
                decision = userDecision,
                command = command,
                date = userFixtures.givenANewDate,
            )

            // Then
            decision.shouldBeInstanceOf<EventToPersist>().event should {
                it.userId shouldBe userId
                it.author shouldBe command.author
                it.sequenceId shouldBe userDecision.nextSequenceId
                it.preferences shouldBe Preferences(language = Locale.ENGLISH)
            }
        }
    }
}
