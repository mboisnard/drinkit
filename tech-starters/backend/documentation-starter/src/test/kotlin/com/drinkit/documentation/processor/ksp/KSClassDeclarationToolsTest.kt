package com.drinkit.documentation.processor.ksp

import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class KSClassDeclarationToolsTest {

    @Test
    fun `the methods the compiler writes for a data class are left out of its page`() {
        // When & Then
        listOf("component1", "component12", "copy", "equals", "hashCode", "toString", "<init>").forAll {
            isGeneratedMethod(it) shouldBe true
        }
    }

    @Test
    fun `a method named after a generated one stays on its page`() {
        // When & Then
        listOf("componentOf", "copyTo", "equalsIgnoringCase").forAll { isGeneratedMethod(it) shouldBe false }
    }
}
