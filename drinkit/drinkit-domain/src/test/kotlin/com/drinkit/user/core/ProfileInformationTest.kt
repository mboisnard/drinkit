package com.drinkit.user.core

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ProfileInformationTest {

    @Test
    fun `the errors of a profile name the broken rule, never the submitted value`() {
        // Given
        val profile = ProfileInformation(
            firstName = FirstName("Jo​sephine"),
            lastName = LastName("Marchetti".repeat(12)),
            birthDate = BirthDate(LocalDate.of(1850, 6, 1)),
        )

        // When
        val errors = profile.validate()

        // Then
        errors shouldHaveSize 3
        errors.joinToString() should {
            it shouldNotContain "sephine"
            it shouldNotContain "Marchetti"
            it shouldNotContain "1850"
        }
    }
}
