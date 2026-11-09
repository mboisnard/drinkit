package com.drinkit.configuration

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal abstract class ConfigurationsTestContract {

    protected val configurations: Configurations by lazy { fetchConfigurations() }

    abstract fun fetchConfigurations(): Configurations

    @Test
    fun `reads the default of a key never set`() {
        // When
        val limits = configurations[TestKeys.limits]

        // Then
        limits shouldBe Limits()
    }

    @Test
    fun `reads back a flag that was set`() {
        // Given
        configurations[TestKeys.enabled] = true

        // When
        val enabled = configurations[TestKeys.enabled]

        // Then
        enabled shouldBe true
    }

    @Test
    fun `reads back a group of settings that was set`() {
        // Given
        configurations[TestKeys.limits] = Limits(maxItems = 42, label = "custom")

        // When
        val limits = configurations[TestKeys.limits]

        // Then
        limits shouldBe Limits(maxItems = 42, label = "custom")
    }

    @Test
    fun `reads back a list of groups that was set`() {
        // Given
        configurations[TestKeys.tags] = listOf(Tag("red"), Tag("blue"))

        // When
        val tags = configurations[TestKeys.tags]

        // Then
        tags shouldBe listOf(Tag("red"), Tag("blue"))
    }

    @Test
    fun `replaces a value that is set again`() {
        // Given
        configurations[TestKeys.limits] = Limits(maxItems = 42)

        // When
        configurations[TestKeys.limits] = Limits(maxItems = 7)

        // Then
        configurations[TestKeys.limits] shouldBe Limits(maxItems = 7)
    }

    @Test
    fun `brings a key back to its default on reset`() {
        // Given
        configurations[TestKeys.limits] = Limits(maxItems = 42)

        // When
        configurations.reset(TestKeys.limits)

        // Then
        configurations[TestKeys.limits] shouldBe Limits()
    }

    data class Limits(val maxItems: Int = 10, val label: String = "none") {
        init {
            require(maxItems in 1..100) { "maxItems must be between 1 and 100" }
        }
    }

    data class Tag(val name: String)

    object TestKeys {
        val enabled = configurationKey("test.enabled", default = false)
        val limits = configurationKey("test.limits", default = Limits())
        val tags = configurationKey("test.tags", default = emptyList<Tag>())
        val label = configurationKey("test.label", default = "none")
    }
}
