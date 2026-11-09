package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.NoOpKSPLogger
import io.kotest.matchers.collections.shouldContainAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

internal class CreateTechStartersOverviewDocumentationTest {

    @Test
    fun `the overview keeps every module when the modules write it at the same time`(
        @TempDir tempDir: File,
    ) {
        // Given
        val modules = (1..16).map { "module-$it-starter" }
        val start = CountDownLatch(1)

        // When
        modules.map { module ->
            thread {
                start.await()
                CreateTechStartersOverviewDocumentation(tempDir.absolutePath, NoOpKSPLogger()).invoke(module, 1)
            }
        }.also { start.countDown() }.forEach { it.join() }

        // Then
        File(tempDir, "tech-starters.md").readLines() shouldContainAll
            modules.map { "- [$it](./$it.md) (1 tool)" }
    }
}
