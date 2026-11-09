package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.NoOpKSPLogger
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CreateTechStarterDocumentationTest {

    @Test
    fun `show a function tool with its signature and its description`(
        @TempDir tempDir: File,
    ) {
        // Given
        val configurationKey = TechStarterTopLevelInfo(
            packageName = "com.drinkit.configuration",
            name = "configurationKey",
            customName = null,
            signature = "fun <T> configurationKey(name: String, default: T): ConfigurationKey<T>",
            description = "Builds the key of a setting.",
        )

        // When
        CreateTechStarterDocumentation(tempDir.absolutePath, tempDir.absolutePath, NoOpKSPLogger())
            .invoke("configuration-starter", emptyList(), listOf(configurationKey))

        // Then
        File(tempDir, "configuration-starter.md").readText() shouldContain """
            |### configurationKey
            |
            |> Builds the key of a setting.
            |
            |**Function**: `com.drinkit.configuration.configurationKey`
            |
            |```kotlin
            |fun <T> configurationKey(name: String, default: T): ConfigurationKey<T>
            |```
        """.trimMargin()
    }

    @Test
    fun `show a property tool with its type and its description`(
        @TempDir tempDir: File,
    ) {
        // Given
        val emailPattern = TechStarterTopLevelInfo(
            packageName = "com.drinkit.kotlin",
            name = "VALID_EMAIL_REGEX",
            customName = null,
            signature = "val VALID_EMAIL_REGEX: Regex",
            description = "The OWASP pattern of an email address.",
            kind = "Property",
        )

        // When
        CreateTechStarterDocumentation(tempDir.absolutePath, tempDir.absolutePath, NoOpKSPLogger())
            .invoke("kotlin-starter", emptyList(), listOf(emailPattern))

        // Then
        File(tempDir, "kotlin-starter.md").readText() shouldContain """
            |### VALID_EMAIL_REGEX
            |
            |> The OWASP pattern of an email address.
            |
            |**Property**: `com.drinkit.kotlin.VALID_EMAIL_REGEX`
            |
            |```kotlin
            |val VALID_EMAIL_REGEX: Regex
            |```
        """.trimMargin()
    }
}
