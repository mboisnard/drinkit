package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.CoreDomainInfo
import com.drinkit.documentation.processor.ksp.NoOpKSPLogger
import com.drinkit.documentation.processor.ksp.UseCaseInfo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CreateCoreDomainDocumentationTest {

    @TempDir
    private lateinit var tempDir: File
    private val logger = NoOpKSPLogger()
    private val createCoreDomainDocumentation by lazy {
        CreateCoreDomainDocumentation(tempDir.absolutePath, logger)
    }

    @Test
    fun `generate markdown file with domain name and description`() {
        // Given
        val domain = CoreDomainInfo(
                packageName = "com.drinkit.user",
                className = "UserManagement",
                customName = null,
                description = "This is a user management domain"
        )

        // When
        createCoreDomainDocumentation.invoke(domain, emptyList())

        // Then
        val generatedFile = File(tempDir, "user-management.md")
        generatedFile.exists() shouldBe true

        val content = generatedFile.readText()
        content shouldContain "# User Management"
        content shouldContain "This is a user management domain"
        content shouldContain "*No use cases found for this core domain.*"
    }

    @Test
    fun `generate markdown file without description when not provided`() {
        // Given
        val domain = CoreDomainInfo(
                packageName = "com.drinkit.user",
                className = "User",
                customName = null,
                description = null
        )

        // When
        createCoreDomainDocumentation.invoke(domain, emptyList())

        // Then
        val generatedFile = File(tempDir, "user.md")
        val content = generatedFile.readText()
        content shouldContain "# User"
        content shouldContain "*No use cases found for this core domain.*"
    }

    @Test
    fun `generate markdown file with use cases sorted by display name`() {
        // Given
        val domain = CoreDomainInfo(
                packageName = "com.drinkit.user",
                className = "User",
                customName = null,
                description = "User domain"
        )

        val useCases = listOf(
                UseCaseInfo(
                        packageName = "com.drinkit.user",
                        className = "UpdateUser",
                        customName = null,
                        description = "Updates an existing user"
                ),
                UseCaseInfo(
                        packageName = "com.drinkit.user",
                        className = "CreateUser",
                        customName = null,
                        description = "Creates a new user"
                ),
                UseCaseInfo(
                        packageName = "com.drinkit.user",
                        className = "DeleteUser",
                        customName = null,
                        description = null
                )
        )

        // When
        createCoreDomainDocumentation.invoke(domain, useCases)

        // Then
        val generatedFile = File(tempDir, "user.md")
        val content = generatedFile.readText()

        content shouldContain "# User"
        content shouldContain "User domain"

        content shouldContain "### Create User"
        content shouldContain "Creates a new user"
        content shouldContain "**Class:** `com.drinkit.user.CreateUser`"

        content shouldContain "### Delete User"
        content shouldContain "**Class:** `com.drinkit.user.DeleteUser`"

        content shouldContain "### Update User"
        content shouldContain "Updates an existing user"
        content shouldContain "**Class:** `com.drinkit.user.UpdateUser`"

        // Verify sorting by checking order
        val createUserIndex = content.indexOf("### Create User")
        val deleteUserIndex = content.indexOf("### Delete User")
        val updateUserIndex = content.indexOf("### Update User")

        createUserIndex shouldBe content.indexOf("### Create User")
        (createUserIndex < deleteUserIndex) shouldBe true
        (deleteUserIndex < updateUserIndex) shouldBe true
    }

    @Test
    fun `should generate markdown file with use case without description`() {
        // Given
        val domain = CoreDomainInfo(
                packageName = "com.drinkit.user",
                className = "User",
                customName = null,
                description = null
        )

        val useCases = listOf(
                UseCaseInfo(
                        packageName = "com.drinkit.user",
                        className = "CreateUser",
                        customName = null,
                        description = null
                )
        )

        // When
        createCoreDomainDocumentation.invoke(domain, useCases)

        // Then
        val generatedFile = File(tempDir, "user.md")
        val content = generatedFile.readText()

        content shouldContain "### Create User"
        content shouldContain "**Class:** `com.drinkit.user.CreateUser`"
    }

    @Test
    fun `should create output directory if it does not exist`() {
        // Given
        val nonExistentDir = File(tempDir, "nested/path/to/output")
        val createFile = CreateCoreDomainDocumentation(nonExistentDir.absolutePath, logger)

        val domain = CoreDomainInfo(
                packageName = "com.drinkit.user",
                className = "User",
                customName = null,
                description = null
        )

        // When
        createFile.invoke(domain, emptyList())

        // Then
        nonExistentDir.exists() shouldBe true
        nonExistentDir.isDirectory shouldBe true
        File(nonExistentDir, "user.md").exists() shouldBe true
    }

    @Test
    fun `should use custom name in title when provided`() {
        // Given
        val domain = CoreDomainInfo(
                packageName = "com.drinkit.user",
                className = "UserManagement",
                customName = "Custom User Domain",
                description = null
        )

        // When
        createCoreDomainDocumentation.invoke(domain, emptyList())

        // Then
        val generatedFile = File(tempDir, "custom-user-domain.md")
        generatedFile.exists() shouldBe true

        val content = generatedFile.readText()
        content shouldContain "# Custom User Domain"
    }
}