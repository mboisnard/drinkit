package com.drinkit.documentation.processor.ksp

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class UseCaseInfoTest {

    @Test
    fun `match domain when className contains domain displayName`() {
        // Given
        val domain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCase = UseCaseInfo(
            packageName = "com.drinkit.application",
            className = "CreateUserProfile",
            customName = null,
            description = null
        )

        // When
        val matches = useCase.matchesDomain(domain)

        // Then
        matches shouldBe true
    }

    @Test
    fun `not match domain when className does not contain domain displayName`() {
        // Given
        val domain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCase = UseCaseInfo(
            packageName = "com.drinkit.application",
            className = "CreateOrder",
            customName = null,
            description = null
        )

        // When
        val matches = useCase.matchesDomain(domain)

        // Then
        matches shouldBe false
    }

    @Test
    fun `match domain case insensitively`() {
        // Given
        val domain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCase = UseCaseInfo(
            packageName = "com.drinkit.application",
            className = "UpdateUSER",
            customName = null,
            description = null
        )

        // When
        val matches = useCase.matchesDomain(domain)

        // Then
        matches shouldBe true
    }

    @Test
    fun `use displayName when customName is null`() {
        // Given & When
        val useCase = UseCaseInfo(
            packageName = "com.drinkit.user",
            className = "CreateUser",
            customName = null,
            description = null
        )

        // Then
        useCase.displayName shouldBe "Create User"
    }

    @Test
    fun `use customName as displayName when provided`() {
        // Given & When
        val useCase = UseCaseInfo(
            packageName = "com.drinkit.user",
            className = "CreateUser",
            customName = "Create New User Account",
            description = null
        )

        // Then
        useCase.displayName shouldBe "Create New User Account"
    }
}
