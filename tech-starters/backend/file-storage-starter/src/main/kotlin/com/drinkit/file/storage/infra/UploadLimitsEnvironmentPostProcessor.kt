package com.drinkit.file.storage.infra

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.getProperty
import org.springframework.util.unit.DataSize

// The multipart fields and boundaries around the file
private val REQUEST_MARGIN = DataSize.ofMegabytes(1)

/**
 * Lets the largest file of a usage through Spring's multipart limits and Tomcat's, so that a larger one answers 413
 * rather than a connection reset.
 */
internal class UploadLimitsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        if (!environment.getProperty<Boolean>("starters.file-storage.enabled", true)) {
            return
        }

        val usageType = Bindable.mapOf(String::class.java, FileStorageProperties.Usage::class.java)
        val usages = Binder.get(environment).bind("starters.file-storage.usages", usageType).orElse(null)
        val largestFile = usages?.values?.maxOfOrNull { it.maxSize.toBytes() } ?: return
        val largestRequest = largestFile + REQUEST_MARGIN.toBytes()

        // Last, below every property source of the application
        environment.propertySources.addLast(
            MapPropertySource(
                "file-storage-starter-upload-limits",
                mapOf(
                    "spring.servlet.multipart.max-file-size" to "${largestFile}B",
                    "spring.servlet.multipart.max-request-size" to "${largestRequest}B",
                    "server.tomcat.max-swallow-size" to "${largestRequest}B",
                ),
            ),
        )
    }
}
