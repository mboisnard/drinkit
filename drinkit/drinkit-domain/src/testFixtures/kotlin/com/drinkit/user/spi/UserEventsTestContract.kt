package com.drinkit.user.spi

import com.drinkit.event.sourcing.transaction.DuplicateSequenceException
import com.drinkit.user.DeleteUserCommand
import com.drinkit.user.ReactivateUserCommand
import com.drinkit.user.UserFixtures
import com.drinkit.user.core.Author
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.ProfileCompleted
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.PromotedAsAdmin
import com.drinkit.user.core.UserHistory
import com.drinkit.user.withDeleted
import com.drinkit.user.withReactivated
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.time.LocalDate

abstract class UserEventsTestContract {

    abstract fun fetchUsers(): Users
    abstract fun fetchUserEvents(users: Users): UserEvents

    private val userFixtures = UserFixtures()
    private val users: Users by lazy { fetchUsers() }
    private val userEvents: UserEvents by lazy { fetchUserEvents(users) }

    @Test
    fun `should save a user history starting by an initialized event`() {
        // Given
        val userInitialized = userFixtures.givenAUserHistory().initEvent

        // When
        userEvents.save(userInitialized)

        // Then
        userEvents.findAllBy(userInitialized.userId).shouldNotBeNull() shouldBe UserHistory(userInitialized)

        val savedUser = users.findEnabledBy(userInitialized.userId)
        savedUser.shouldNotBeNull() should {
            it.email shouldBe userInitialized.email
            it.roles shouldBe userInitialized.roles
        }
    }

    @Test
    fun `keeps a deleted then reactivated user enabled`() {
        // Given
        val history = userFixtures.givenAUserHistory()
        val author = Author.Connected(history.initEvent.userId)
        val deleted = history.withDeleted(DeleteUserCommand(author), date = userFixtures.givenANewDate)
        val reactivated = deleted.withReactivated(ReactivateUserCommand(author), date = userFixtures.givenANewDate)

        // When
        (listOf(reactivated.initEvent) + reactivated.remainingEvents).forEach { userEvents.save(it) }

        // Then
        userEvents.findAllBy(history.initEvent.userId) shouldBe reactivated
        users.findEnabledBy(history.initEvent.userId).shouldNotBeNull()
    }

    @Test
    fun `an event at a sequence id already stored is refused`() {
        // Given
        val userInitialized = userFixtures.givenAUserHistory().initEvent
        userEvents.save(userInitialized)

        val promotedAtTheSameSequenceId = PromotedAsAdmin(
            userId = userInitialized.userId,
            date = userFixtures.givenANewDate,
            author = Author.Connected(userInitialized.userId),
            sequenceId = userInitialized.sequenceId,
        )

        // When / Then
        shouldThrow<DuplicateSequenceException> { userEvents.save(promotedAtTheSameSequenceId) }
    }

    @Test
    fun `the refusal of a taken sequence id carries no personal data`() {
        // Given
        val userInitialized = userFixtures.givenAUserHistory().initEvent
        userEvents.save(userInitialized)

        val profileCompletedAtTheSameSequenceId = ProfileCompleted(
            userId = userInitialized.userId,
            date = userFixtures.givenANewDate,
            author = Author.Connected(userInitialized.userId),
            sequenceId = userInitialized.sequenceId,
            profile = ProfileInformation(
                firstName = FirstName("Josephine"),
                lastName = LastName("Marchetti"),
                birthDate = BirthDate(LocalDate.of(1987, 3, 14)),
            ),
        )

        // When
        val refusal = shouldThrow<DuplicateSequenceException> {
            userEvents.save(profileCompletedAtTheSameSequenceId)
        }

        // Then
        refusal.stackTraceToString() should {
            it shouldNotContain "Josephine"
            it shouldNotContain "Marchetti"
            it shouldNotContain "1987-03-14"
        }
    }
}
