package com.drinkit.file.storage.infra

import com.drinkit.file.storage.infra.files.FileUsage
import com.drinkit.file.storage.infra.s3.S3Settings
import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitRule
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.nio.file.Path
import java.time.Duration

// A browser runs the script such a file holds when it opens it from the application's origin
private val SCRIPT_TYPES = setOf(
    "image/svg+xml",
    "text/html",
    "application/xhtml+xml",
    "application/xml",
    "text/xml",
    "application/javascript",
    "text/javascript",
)

@ConfigurationProperties(prefix = "starters.file-storage")
internal data class FileStorageProperties(
    val enabled: Boolean = true,
    val provider: Provider = Provider.FILESYSTEM,
    val usages: Map<String, Usage> = emptyMap(),
    val temporaryRetention: Duration = DEFAULT_RETENTION,
    val urlTtl: Duration = DEFAULT_URL_TTL,
    val rateLimit: UploadRateLimit = UploadRateLimit(),
    val readAllRoles: Set<String> = setOf("ROLE_ADMIN"),
    val filesystem: Filesystem = Filesystem(),
    val s3: S3Settings = S3Settings(),
) {
    init {
        usages.forEach { (name, usage) ->
            val scripts = usage.types intersect SCRIPT_TYPES
            require(scripts.isEmpty()) { "The file usage $name allows $scripts, which can carry a script" }
        }
        require(provider != Provider.S3 || s3.bucket.isNotBlank()) {
            "The s3 provider needs starters.file-storage.s3.bucket"
        }
    }

    fun fileUsages(): Map<String, FileUsage> =
        usages.mapValues { (_, usage) -> FileUsage(usage.types, usage.maxSize.toBytes()) }

    fun publicUsages(): Set<String> = usages.filterValues { it.visibility == Visibility.PUBLIC }.keys

    enum class Provider { FILESYSTEM, S3 }

    enum class Visibility { PUBLIC, PRIVATE }

    data class Usage(
        val types: Set<String> = emptySet(),
        val maxSize: DataSize = DEFAULT_MAX_SIZE,
        val visibility: Visibility = Visibility.PRIVATE,
    )

    data class UploadRateLimit(
        val perTenMinutes: Long = DEFAULT_UPLOADS_PER_TEN_MINUTES,
        val perDay: Long = DEFAULT_UPLOADS_PER_DAY,
    ) {
        fun rule() = RateLimitRule(
            "file-upload",
            listOf(RateLimit(perTenMinutes, TEN_MINUTES), RateLimit(perDay, Duration.ofDays(1))),
        )
    }

    // A String, since Spring reads a Path as a resource of the servlet context, which refuses a relative "../"
    data class Filesystem(
        val directory: String = Path.of(System.getProperty("java.io.tmpdir"), "drinkit-files").toString(),
        val signingKey: String = "",
    ) {
        override fun toString() = "Filesystem(directory=$directory)"
    }

    private companion object {
        val DEFAULT_RETENTION: Duration = Duration.ofHours(24)
        val DEFAULT_URL_TTL: Duration = Duration.ofMinutes(5)
        val DEFAULT_MAX_SIZE: DataSize = DataSize.ofMegabytes(5)
        const val DEFAULT_UPLOADS_PER_TEN_MINUTES = 20L
        const val DEFAULT_UPLOADS_PER_DAY = 200L
        val TEN_MINUTES: Duration = Duration.ofMinutes(10)
    }
}
