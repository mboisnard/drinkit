package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.NoOpKSPLogger
import com.drinkit.documentation.processor.ksp.TechStarterFunctionInfo
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CreateTechStarterDocumentationTest {
    @Test
    fun `show a function tool with its signature and its description`(
        @TempDir tempDir: File,
    ) {
        val configurationKey = TechStarterFunctionInfo(
            packageName = "com.drinkit.configuration",
            name = "configurationKey",
            customName = null,
            signature = "fun <T> configurationKey(name: String, default: T): ConfigurationKey<T>",
            description = "Builds the key of a setting.",
        )

        CreateTechStarterDocumentation(tempDir.absolutePath, tempDir.absolutePath, NoOpKSPLogger())
            .invoke("configuration-starter", emptyList(), listOf(configurationKey))

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
}
