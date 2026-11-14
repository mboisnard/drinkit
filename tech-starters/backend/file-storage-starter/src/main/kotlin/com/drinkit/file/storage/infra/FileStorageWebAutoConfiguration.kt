package com.drinkit.file.storage.infra

import com.drinkit.file.storage.infra.files.FileCleanUp
import com.drinkit.file.storage.infra.files.FileUploadApi
import com.drinkit.file.storage.infra.files.FileUploads
import com.drinkit.file.storage.infra.files.PUBLIC_FILE_PATH
import com.drinkit.file.storage.infra.files.PublicFileApi
import com.drinkit.file.storage.infra.files.Storage
import com.drinkit.file.storage.infra.files.StoredFiles
import com.drinkit.file.storage.infra.files.UPLOAD_PATH
import com.drinkit.file.storage.infra.filesystem.FilesystemStorage
import com.drinkit.file.storage.infra.filesystem.SIGNED_FILE_PATH
import com.drinkit.file.storage.infra.filesystem.SignedFileApi
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.security.CurrentUser
import com.drinkit.security.EndpointRules
import com.drinkit.security.endpointRules
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

// The upload route sits under /api/**, whose rule the application declares: the starter adds none for it
@AutoConfiguration(after = [FileStorageAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(StoredFiles::class)
@ConditionalOnProperty(prefix = "starters.file-storage", name = ["enabled"], matchIfMissing = true)
internal class FileStorageWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["fileUploadEndpoint"])
    fun fileUploadEndpoint(
        uploads: FileUploads,
        cleanUp: FileCleanUp,
        rateLimiter: RateLimiter,
        currentUser: CurrentUser,
        properties: FileStorageProperties,
    ): RouterFunction<ServerResponse> {
        val api = FileUploadApi(uploads, cleanUp, rateLimiter, properties.rateLimit.rule(), currentUser)

        return RouterFunctions.route().POST(UPLOAD_PATH, api::upload).build()
    }

    @Bean
    @ConditionalOnMissingBean(name = ["publicFileEndpoint"])
    fun publicFileEndpoint(files: StoredFiles, storage: Storage): RouterFunction<ServerResponse> =
        RouterFunctions.route().GET(PUBLIC_FILE_PATH, PublicFileApi(files, storage)::serve).build()

    // Out of /api/**, so that a broad rule of the application never decides for it: the usage makes a file public
    @Bean
    @ConditionalOnMissingBean(name = ["publicFileAccessRules"])
    fun publicFileAccessRules(): EndpointRules = endpointRules {
        authorize(HttpMethod.GET, "/files/public/*", permitAll)
    }

    @Bean
    @ConditionalOnBean(FilesystemStorage::class)
    @ConditionalOnMissingBean(name = ["signedFileEndpoint"])
    fun signedFileEndpoint(
        storage: FilesystemStorage,
        properties: FileStorageProperties,
    ): RouterFunction<ServerResponse> =
        RouterFunctions.route().GET(SIGNED_FILE_PATH, SignedFileApi(storage, properties.urlTtl)::serve).build()

    // Out of /api/**, so that a broad rule of the application never decides for it: the signature is the credential
    @Bean
    @ConditionalOnBean(FilesystemStorage::class)
    @ConditionalOnMissingBean(name = ["signedFileAccessRules"])
    fun signedFileAccessRules(): EndpointRules = endpointRules {
        authorize(HttpMethod.GET, "/files/*", permitAll)
    }
}
