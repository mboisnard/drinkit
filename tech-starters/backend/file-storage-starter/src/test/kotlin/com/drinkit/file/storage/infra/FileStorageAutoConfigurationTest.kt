package com.drinkit.file.storage.infra

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileStorage
import com.drinkit.file.storage.InMemoryFileStorage
import com.drinkit.file.storage.infra.files.Storage
import com.drinkit.file.storage.infra.files.StoredFile
import com.drinkit.file.storage.infra.files.StoredFiles
import com.drinkit.file.storage.infra.filesystem.FilesystemStorage
import com.drinkit.file.storage.infra.s3.S3Storage
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.rate.limit.inMemoryRateLimiter
import com.drinkit.security.InMemoryCurrentUser
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.web.server.autoconfigure.ServerProperties
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.PlatformTransactionManager
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

internal class FileStorageAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration::class.java))
        .withBean(DSLContext::class.java, { DSL.using(SQLDialect.POSTGRES) })
        .withBean(Clock::class.java, { Clock.systemUTC() })
        .withBean(InMemoryCurrentUser::class.java, { InMemoryCurrentUser() })
        .withBean(PlatformTransactionManager::class.java, { DataSourceTransactionManager(DriverManagerDataSource()) })
        .withPropertyValues("starters.file-storage.filesystem.directory=${Files.createTempDirectory("files")}")

    private val withS3 = arrayOf(
        "starters.file-storage.provider=s3",
        "starters.file-storage.s3.endpoint=http://localhost:9000",
        "starters.file-storage.s3.bucket=drinkit-files",
        "starters.file-storage.s3.access-key=drinkit",
        "starters.file-storage.s3.secret-key=drinkit-secret",
    )

    @Test
    fun `the files are stored on the filesystem by default`() {
        runner.run { context ->
            // When & Then
            context.getBean<FileStorage>().shouldBeInstanceOf<StoredFiles>()
            context.getBean<Storage>().shouldBeInstanceOf<FilesystemStorage>()
        }
    }

    @Test
    fun `a URL of the filesystem starts with the context path the application is served under`() {
        // Given
        val underDrinkit = runner.withBean(
            ServerProperties::class.java,
            { ServerProperties().apply { servlet.contextPath = "/drinkit" } },
        )
        val id = FileId(UUID.randomUUID().toString())
        val photo = StoredFile(id, "bottle-photo", "ada", "image/jpeg", 1, "photo.jpg", Instant.EPOCH)

        underDrinkit.run { context ->
            // When
            val url = context.getBean<Storage>().urlOf(photo, Duration.ofMinutes(5))

            // Then
            url.path shouldBe "/drinkit/files/${photo.id.value}"
        }
    }

    @Test
    fun `the files are stored in an S3 bucket when the provider is s3`() {
        runner.withPropertyValues(*withS3).run { context ->
            // When & Then
            context.getBean<Storage>().shouldBeInstanceOf<S3Storage>()
        }
    }

    @Test
    fun `an application FileStorage replaces the starter's`() {
        // Given
        val own = InMemoryFileStorage()

        runner.withBean(FileStorage::class.java, { own }).run { context ->
            // When & Then
            context.getBean<FileStorage>() shouldBeSameInstanceAs own
        }
    }

    @Test
    fun `starters file-storage enabled false removes every bean`() {
        // Given
        val disabled = runner.withPropertyValues("starters.file-storage.enabled=false")

        disabled.run { context ->
            // When & Then
            context.getBeansOfType(FileStorage::class.java).keys shouldBe emptySet()
            context.getBeansOfType(Storage::class.java).keys shouldBe emptySet()
        }
    }

    @Test
    fun `the s3 provider refuses to start without a bucket`() {
        // Given
        val withoutBucket = runner.withPropertyValues("starters.file-storage.provider=s3")

        withoutBucket.run { context ->
            // When & Then
            context.startupFailure.shouldNotBeNull().rootCause().message shouldContain "starters.file-storage.s3.bucket"
        }
    }

    @Test
    fun `a usage that allows a type able to carry a script refuses to start`() {
        // Given
        val withSvg = runner.withPropertyValues("starters.file-storage.usages.logo.types=image/png,image/svg+xml")

        withSvg.run { context ->
            // When & Then
            context.startupFailure.shouldNotBeNull().rootCause().message shouldContain "image/svg+xml"
        }
    }

    @Test
    fun `a web application gets the upload and public file routes, and the signed one of the filesystem only`() {
        // When & Then
        webRunner().run { context ->
            context.containsBean("fileUploadEndpoint") shouldBe true
            context.containsBean("publicFileEndpoint") shouldBe true
            context.containsBean("signedFileEndpoint") shouldBe true
        }
        webRunner().withPropertyValues(*withS3).run { context ->
            context.containsBean("signedFileEndpoint") shouldBe false
        }
    }

    private fun webRunner() = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                FileStorageAutoConfiguration::class.java,
                FileStorageWebAutoConfiguration::class.java,
            ),
        )
        .withBean(DSLContext::class.java, { DSL.using(SQLDialect.POSTGRES) })
        .withBean(Clock::class.java, { Clock.systemUTC() })
        .withBean(InMemoryCurrentUser::class.java, { InMemoryCurrentUser() })
        .withBean(PlatformTransactionManager::class.java, { DataSourceTransactionManager(DriverManagerDataSource()) })
        .withBean(RateLimiter::class.java, { inMemoryRateLimiter() })
        .withPropertyValues("starters.file-storage.filesystem.directory=${Files.createTempDirectory("files")}")

    private fun Throwable.rootCause(): Throwable = generateSequence(this) { it.cause }.last()
}
