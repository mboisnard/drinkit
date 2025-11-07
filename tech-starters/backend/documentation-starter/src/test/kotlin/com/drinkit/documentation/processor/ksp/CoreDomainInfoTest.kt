package com.drinkit.documentation.processor.ksp

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class CoreDomainInfoTest {

    @Test
    fun `use className as displayName when customName is null`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "UserManagement",
            customName = null,
            description = "User management core domain"
        )

        // When
        val displayName = coreDomain.displayName

        // Then
        displayName shouldBe "User Management"
    }

    @Test
    fun `use customName as displayName when provided`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "UserManagement",
            customName = "Custom User Name",
            description = "User management core domain"
        )

        // When
        val displayName = coreDomain.displayName

        // Then
        displayName shouldBe "Custom User Name"
    }

    @Test
    fun `convert displayName to lowercase with dashes for fileName`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "UserManagement",
            customName = null,
            description = "User management core domain"
        )

        // When
        val fileName = coreDomain.fileName

        // Then
        fileName shouldBe "user-management"
    }

    @Test
    fun `find usecases in the same package - Strategy 1`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCaseInSamePackage = UseCaseInfo(
            packageName = "com.drinkit.user",
            className = "CreateUser",
            customName = null,
            description = null
        )

        val useCaseInOtherPackage = UseCaseInfo(
            packageName = "com.drinkit.order",
            className = "CreateOrder",
            customName = null,
            description = null
        )

        val useCasesByPackage = mapOf(
            "com.drinkit.user" to listOf(useCaseInSamePackage),
            "com.drinkit.order" to listOf(useCaseInOtherPackage)
        )

        // When
        val result = coreDomain.findAssociatedUseCases(useCasesByPackage)

        // Then
        result.shouldContainExactly(useCaseInSamePackage)
    }

    @Test
    fun `find usecases in parent package when package contains domain name - Strategy 2`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.user.domain",
            className = "User",
            customName = null,
            description = null
        )

        val useCaseInParentPackage = UseCaseInfo(
            packageName = "com.drinkit.user",
            className = "CreateInformation",
            customName = null,
            description = null
        )

        val useCasesByPackage = mapOf(
            "com.drinkit.user" to listOf(useCaseInParentPackage)
        )

        // When
        val result = coreDomain.findAssociatedUseCases(useCasesByPackage)

        // Then
        result.shouldContainExactly(useCaseInParentPackage)
    }

    @Test
    fun `find usecases in parent package by matching domain name in usecase class name - Strategy 3`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.domain.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCaseMatchingDomainName = UseCaseInfo(
            packageName = "com.drinkit.domain",
            className = "CreateUser",
            customName = null,
            description = null
        )

        val useCaseNotMatchingDomainName = UseCaseInfo(
            packageName = "com.drinkit.domain",
            className = "CreateOrder",
            customName = null,
            description = null
        )

        val useCasesByPackage = mapOf(
            "com.drinkit.domain" to listOf(useCaseMatchingDomainName, useCaseNotMatchingDomainName)
        )

        // When
        val result = coreDomain.findAssociatedUseCases(useCasesByPackage)

        // Then
        result.shouldContainExactly(useCaseMatchingDomainName)
    }

    @Test
    fun `find usecases globally by name matching - Strategy 4`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.domain.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCaseMatchingDomainName1 = UseCaseInfo(
            packageName = "com.drinkit.application",
            className = "CreateUser",
            customName = null,
            description = null
        )

        val useCaseMatchingDomainName2 = UseCaseInfo(
            packageName = "com.drinkit.service",
            className = "UpdateUser",
            customName = null,
            description = null
        )

        val useCaseNotMatching = UseCaseInfo(
            packageName = "com.drinkit.order",
            className = "CreateOrder",
            customName = null,
            description = null
        )

        val useCasesByPackage = mapOf(
            "com.drinkit.application" to listOf(useCaseMatchingDomainName1),
            "com.drinkit.service" to listOf(useCaseMatchingDomainName2),
            "com.drinkit.order" to listOf(useCaseNotMatching)
        )

        // When
        val result = coreDomain.findAssociatedUseCases(useCasesByPackage)

        // Then
        result.shouldContainExactlyInAnyOrder(useCaseMatchingDomainName1, useCaseMatchingDomainName2)
    }

    @Test
    fun `empty list when no usecases are found`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.user",
            className = "User",
            customName = null,
            description = null
        )

        val useCasesByPackage = mapOf(
            "com.drinkit.order" to listOf(
                UseCaseInfo(
                    packageName = "com.drinkit.order",
                    className = "CreateOrder",
                    customName = null,
                    description = null
                )
            )
        )

        // When
        val result = coreDomain.findAssociatedUseCases(useCasesByPackage)

        // Then
        result shouldBe emptyList()
    }

    @Test
    fun `should handle displayName with custom name for matching`() {
        // Given
        val coreDomain = CoreDomainInfo(
            packageName = "com.drinkit.domain",
            className = "UserManagement",
            customName = "UserDomain",
            description = null
        )

        val useCaseMatchingCustomName = UseCaseInfo(
            packageName = "com.drinkit.application",
            className = "CreateUserDomain",
            customName = null,
            description = null
        )

        val useCasesByPackage = mapOf(
            "com.drinkit.application" to listOf(useCaseMatchingCustomName)
        )

        // When
        val result = coreDomain.findAssociatedUseCases(useCasesByPackage)

        // Then
        result shouldBe listOf(useCaseMatchingCustomName)
    }
}
