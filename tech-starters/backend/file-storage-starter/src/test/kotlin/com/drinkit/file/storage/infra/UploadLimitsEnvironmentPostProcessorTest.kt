package com.drinkit.file.storage.infra

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

internal class UploadLimitsEnvironmentPostProcessorTest {

    @Test
    fun `the multipart limits let the largest file of a usage through`() {
        // When
        val environment = withLimits(
            "starters.file-storage.usages.bottle-photo.max-size" to "10MB",
            "starters.file-storage.usages.label-scan.max-size" to "20MB",
        )

        // Then
        environment.getProperty("spring.servlet.multipart.max-file-size") shouldBe "${20L * MEGABYTE}B"
        environment.getProperty("spring.servlet.multipart.max-request-size") shouldBe "${21L * MEGABYTE}B"
        environment.getProperty("server.tomcat.max-swallow-size") shouldBe "${21L * MEGABYTE}B"
    }

    @Test
    fun `the application's own multipart limit wins`() {
        // When
        val environment = withLimits(
            "starters.file-storage.usages.bottle-photo.max-size" to "10MB",
            "spring.servlet.multipart.max-file-size" to "1MB",
        )

        // Then
        environment.getProperty("spring.servlet.multipart.max-file-size") shouldBe "1MB"
    }

    @Test
    fun `no usage changes no limit`() {
        // When
        val environment = withLimits()

        // Then
        environment.getProperty("spring.servlet.multipart.max-file-size") shouldBe null
    }

    private fun withLimits(vararg applicationProperties: Pair<String, String>): StandardEnvironment {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("application", applicationProperties.toMap()))

        UploadLimitsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())

        return environment
    }

    private companion object {
        const val MEGABYTE = 1024L * 1024L
    }
}
