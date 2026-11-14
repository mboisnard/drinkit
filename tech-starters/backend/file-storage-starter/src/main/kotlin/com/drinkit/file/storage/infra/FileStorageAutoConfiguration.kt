package com.drinkit.file.storage.infra

import com.drinkit.file.storage.FileAccess
import com.drinkit.file.storage.FileStorage
import com.drinkit.file.storage.infra.files.FileCleanUp
import com.drinkit.file.storage.infra.files.FileRights
import com.drinkit.file.storage.infra.files.FileUploads
import com.drinkit.file.storage.infra.files.Storage
import com.drinkit.file.storage.infra.files.StoredFileMetadata
import com.drinkit.file.storage.infra.files.StoredFiles
import com.drinkit.file.storage.infra.files.UploadRules
import com.drinkit.file.storage.infra.files.jooq.JooqStoredFileMetadata
import com.drinkit.file.storage.infra.filesystem.FilesystemStorage
import com.drinkit.file.storage.infra.s3.S3Storage
import com.drinkit.security.CurrentUser
import org.jooq.DSLContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.web.server.autoconfigure.ServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Clock

private const val GENERATED_KEY_BYTES = 32

@AutoConfiguration(after = [JooqAutoConfiguration::class])
@ConditionalOnBean(DSLContext::class)
@ConditionalOnProperty(prefix = "starters.file-storage", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(FileStorageProperties::class)
internal class FileStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun storedFileMetadata(dsl: DSLContext): StoredFileMetadata = JooqStoredFileMetadata(dsl)

    // An unset key is drawn at each start: the URLs of one instance then die with it
    @Bean
    @ConditionalOnMissingBean(Storage::class)
    @ConditionalOnProperty(
        prefix = "starters.file-storage",
        name = ["provider"],
        havingValue = "filesystem",
        matchIfMissing = true,
    )
    fun filesystemStorage(
        properties: FileStorageProperties,
        clock: Clock,
        server: ObjectProvider<ServerProperties>,
    ): FilesystemStorage {
        val configuredKey = properties.filesystem.signingKey.toByteArray()
        val key = configuredKey.takeIf { it.isNotEmpty() }
            ?: ByteArray(GENERATED_KEY_BYTES).also(SecureRandom()::nextBytes)

        return FilesystemStorage(Path.of(properties.filesystem.directory), key, clock, server.contextPath())
    }

    @Bean
    @ConditionalOnMissingBean(Storage::class)
    @ConditionalOnProperty(prefix = "starters.file-storage", name = ["provider"], havingValue = "s3")
    fun s3Storage(properties: FileStorageProperties): S3Storage = S3Storage.of(properties.s3)

    @Bean
    @ConditionalOnMissingBean
    fun fileRights(
        properties: FileStorageProperties,
        accesses: ObjectProvider<FileAccess>,
        server: ObjectProvider<ServerProperties>,
    ) = FileRights(
        properties.publicUsages(),
        properties.readAllRoles,
        accesses.orderedStream().toList(),
        server.contextPath(),
    )

    @Bean
    @ConditionalOnMissingBean
    fun fileUploads(
        metadata: StoredFileMetadata,
        storage: Storage,
        currentUser: CurrentUser,
        clock: Clock,
        properties: FileStorageProperties,
    ) = FileUploads(metadata, storage, UploadRules(properties.fileUsages()), currentUser, clock)

    @Bean
    @ConditionalOnMissingBean
    fun fileCleanUp(
        metadata: StoredFileMetadata,
        storage: Storage,
        clock: Clock,
        properties: FileStorageProperties,
        transactionManager: PlatformTransactionManager,
    ) = FileCleanUp(metadata, storage, clock, properties.temporaryRetention, TransactionTemplate(transactionManager))

    @Bean
    @ConditionalOnMissingBean(FileStorage::class)
    @Suppress("LongParameterList") // Each one is a bean the lifecycle needs, the settings come with the properties
    fun fileStorage(
        uploads: FileUploads,
        metadata: StoredFileMetadata,
        storage: Storage,
        rights: FileRights,
        currentUser: CurrentUser,
        properties: FileStorageProperties,
    ) = StoredFiles(uploads, metadata, storage, rights, currentUser, properties.urlTtl)

    private fun ObjectProvider<ServerProperties>.contextPath() = getIfAvailable()?.servlet?.contextPath.orEmpty()
}
