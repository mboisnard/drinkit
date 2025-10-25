package com.drinkit.configuration.infra

import com.drinkit.configuration.ConfigurationKey
import com.drinkit.configuration.Configurations
import com.drinkit.configuration.get
import com.drinkit.configuration.getOrSetDefault
import com.drinkit.configuration.getOrThrow
import com.drinkit.configuration.set
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal abstract class ConfigurationsTestContract {

    protected val repository: Configurations by lazy { fetchRepository() }

    abstract fun fetchRepository(): Configurations

    @Test
    fun `returns null when configuration does not exist`() {
        // When
        val result = repository.get(TestKey.Feature1)

        // Then
        result.shouldBeNull()
    }

    @Test
    fun `stores and retrieves boolean configuration`() {
        // Given
        repository.set(TestKey.Feature1, true)

        // When
        val result = repository.get(TestKey.Feature1)

        // Then
        result shouldBe true
    }

    @Test
    fun `stores and retrieves string configuration`() {
        // Given
        repository.set(TestKey.Name, "test-name")

        // When
        val result = repository.get(TestKey.Name)

        // Then
        result shouldBe "test-name"
    }

    @Test
    fun `stores and retrieves integer configuration`() {
        // Given
        repository.set(TestKey.MaxItems, 42)

        // When
        val result = repository.get(TestKey.MaxItems)

        result shouldBe 42
    }

    @Test
    fun `stores and retrieves complex object`() {
        // Given
        val complexConfig = TestComplexConfig(
            host = "localhost",
            port = 5432,
            enabled = true
        )
        repository.set(TestKey.ComplexConfig, complexConfig)

        // When
        val result = repository.get(TestKey.ComplexConfig)

        // Then
        result shouldBe complexConfig
    }

    @Test
    fun `getOrSetDefault persists the default value when configuration does not exist`() {
        // Given
        repository.getOrSetDefault(TestKey.MaxItems, 100)

        // When
        val result = repository.get(TestKey.MaxItems)

        // Then
        result shouldBe 100
    }

    @Test
    fun `getOrSetDefault returns existing value when configuration exists`() {
        // Given
        repository.set(TestKey.MaxItems, 42)

        // When
        val result = repository.getOrSetDefault(TestKey.MaxItems, 100)

        // Then
        result shouldBe 42
    }

    @Test
    fun `throws exception when configuration does not exist and getOrThrow is used`() {
        assertThrows<NoSuchElementException> {
            repository.getOrThrow(TestKey.Feature1)
        }
    }

    @Test
    fun `deletes configuration`() {
        // Given
        repository.set(TestKey.Feature1, true)

        // When
        repository.delete(TestKey.Feature1)

        // Then
        val result = repository.get(TestKey.Feature1)
        result.shouldBeNull()
    }

    @Test
    fun `updates existing configuration`() {
        // Given
        repository.set(TestKey.MaxItems, 42)

        // When
        repository.set(TestKey.MaxItems, 100)
        val result = repository.get(TestKey.MaxItems)

        // Then
        result shouldBe 100
    }

    object TestKey {
        object Feature1 : ConfigurationKey<Boolean>
        object MaxItems : ConfigurationKey<Int>
        object Name : ConfigurationKey<String>
        object ComplexConfig : ConfigurationKey<TestComplexConfig>
    }

    data class TestComplexConfig(
        val host: String,
        val port: Int,
        val enabled: Boolean
    )
}