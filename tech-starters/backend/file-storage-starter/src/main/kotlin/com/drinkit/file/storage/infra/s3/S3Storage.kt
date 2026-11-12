package com.drinkit.file.storage.infra.s3

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.infra.files.Storage
import com.drinkit.file.storage.infra.files.StoredFile
import org.springframework.core.io.InputStreamSource
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.LegacyMd5Plugin
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.InputStream
import java.net.URI
import java.time.Duration

/** Where the bucket is, and the credentials of its access key. */
internal data class S3Settings(
    val endpoint: URI? = null,
    val region: String = "auto",
    val bucket: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
) {
    override fun toString() = "S3Settings(endpoint=$endpoint, region=$region, bucket=$bucket)"
}

/** Files in a private bucket of any S3-compatible store, read through presigned URLs. */
internal class S3Storage(private val client: S3Client, private val presigner: S3Presigner, private val bucket: String) :
    Storage,
    AutoCloseable {

    override fun put(file: StoredFile, content: InputStreamSource) {
        content.inputStream.use { bytes ->
            client.putObject(
                {
                    it.bucket(bucket)
                        .key(file.id.value)
                        .contentType(file.type)
                        .contentDisposition(file.contentDisposition())
                },
                RequestBody.fromInputStream(bytes, file.size),
            )
        }
    }

    override fun read(file: StoredFile): InputStream = client.getObject { it.bucket(bucket).key(file.id.value) }

    override fun delete(ids: List<FileId>) {
        val objects = ids.map { ObjectIdentifier.builder().key(it.value).build() }

        val response =
            client.deleteObjects { it.bucket(bucket).delete(Delete.builder().objects(objects).quiet(true).build()) }

        // The rows go only once their objects are gone, or nothing would retry them
        check(response.errors().isEmpty()) {
            "The bucket kept ${response.errors().size} files: ${response.errors().map { it.code() }.distinct()}"
        }
    }

    override fun urlOf(file: StoredFile, ttl: Duration): URI = presigner.presignGetObject { presign ->
        presign.signatureDuration(ttl).getObjectRequest {
            it.bucket(bucket)
                .key(file.id.value)
                .responseContentType(file.type)
                .responseContentDisposition(file.contentDisposition())
        }
    }.url().toURI()

    override fun close() {
        presigner.close()
        client.close()
    }

    companion object {

        // Path style, no chunked encoding and checksums only when required: what R2, Garage and GCS accept
        fun of(settings: S3Settings): S3Storage {
            val credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(settings.accessKey, settings.secretKey),
            )
            val configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build()
            val region = Region.of(settings.region)

            val client = S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(configuration)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .addPlugin(LegacyMd5Plugin.create())
                .apply { settings.endpoint?.let(::endpointOverride) }
                .build()
            val presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(configuration)
                .apply { settings.endpoint?.let(::endpointOverride) }
                .build()

            return S3Storage(client, presigner, settings.bucket)
        }
    }
}
