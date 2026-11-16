package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.NoOpKSPLogger
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CreateCoreDomainsOverviewDocumentationTest {

    private val cellar = CoreDomainInfo(
        packageName = "com.drinkit.cellar",
        className = "Cellar",
        customName = null,
        description = null,
    )

    @Test
    fun `the overview is the index page of the domains, titled Domains`(
        @TempDir tempDir: File,
    ) {
        // When
        CreateCoreDomainsOverviewDocumentation(tempDir.absolutePath, NoOpKSPLogger()).invoke(listOf(cellar), emptyMap())

        // Then
        File(tempDir, "index.md").readText() shouldStartWith "---\ntitle: \"Domains\"\n"
    }

    @Test
    fun `the overview links each domain to its page`(
        @TempDir tempDir: File,
    ) {
        // When
        CreateCoreDomainsOverviewDocumentation(tempDir.absolutePath, NoOpKSPLogger()).invoke(listOf(cellar), emptyMap())

        // Then
        File(tempDir, "index.md").readText() shouldContain "- [Cellar](./cellar.md) (0 use cases)"
    }
}
