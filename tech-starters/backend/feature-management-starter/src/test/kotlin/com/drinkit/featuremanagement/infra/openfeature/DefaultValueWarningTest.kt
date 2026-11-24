package com.drinkit.featuremanagement.infra.openfeature

import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
internal class DefaultValueWarningTest {

    @Test
    fun `warns in one line that a flag it cannot evaluate answers its default`(output: CapturedOutput) {
        // Given
        val api = OpenFeatureAPI.createIsolated().apply {
            addHooks(DefaultValueWarning())
            setProviderAndWait(InMemoryProvider(emptyMap()))
        }

        // When
        api.client.getBooleanValue("checkout-test", true)

        // Then
        output.out shouldContain "WARN"
        output.out shouldContain "flag checkout-test not found, default value true used"
    }
}
