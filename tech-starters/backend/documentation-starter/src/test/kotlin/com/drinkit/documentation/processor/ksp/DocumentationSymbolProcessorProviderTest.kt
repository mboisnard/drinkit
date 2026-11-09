package com.drinkit.documentation.processor.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

internal class DocumentationSymbolProcessorProviderTest {

    // The provider writes no source file
    private val codeGenerator = Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(CodeGenerator::class.java),
    ) { _, _, _ -> error("unused") } as CodeGenerator

    @Test
    fun `refuses to run without the folder that receives the pages`() {
        // Given
        val options = mapOf("moduleName" to "kotlin-starter", "moduleSourceDir" to "/kotlin-starter")
        val environment = SymbolProcessorEnvironment(options, KotlinVersion.CURRENT, codeGenerator, NoOpKSPLogger())

        // When
        val failure = shouldThrow<IllegalArgumentException> {
            DocumentationSymbolProcessorProvider().create(environment)
        }

        // Then
        failure.message shouldBe "The KSP option docsOutputDir is missing: apply the documentation-convention plugin"
    }
}
