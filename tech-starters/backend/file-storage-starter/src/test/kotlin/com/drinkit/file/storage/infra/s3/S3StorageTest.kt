package com.drinkit.file.storage.infra.s3

import com.drinkit.file.storage.infra.files.StorageTestContract
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.HttpURLConnection.HTTP_OK
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val RUSTFS_IMAGE_NAME = "rustfs/rustfs:1.0.4"
private const val S3_PORT = 9000

internal class S3StorageTest : StorageTestContract() {

    override val storage = S3Storage.of(SETTINGS)

    private val http = HttpClient.newHttpClient()

    override fun readBack(url: URI): ByteArray? {
        val response = http.send(HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray())

        return response.body().takeIf { response.statusCode() == 200 }
    }

    @Test
    fun `a URL expires after its time to live`() {
        // Given
        val photo = aPhoto()
        storage.put(photo, content())

        // When
        val url = storage.urlOf(photo, TTL)

        // Then
        url.query shouldContain "X-Amz-Expires=${TTL.seconds}"
    }

    private companion object {
        // RustFS checks the signatures, as a bucket in the cloud does
        val RUSTFS: GenericContainer<*> = GenericContainer(DockerImageName.parse(RUSTFS_IMAGE_NAME))
            .withExposedPorts(S3_PORT)
            .withEnv("RUSTFS_ACCESS_KEY", "drinkit")
            .withEnv("RUSTFS_SECRET_KEY", "drinkit-secret")
            .waitingFor(Wait.forHttp("/health").forStatusCode(HTTP_OK))
            .apply { start() }

        val SETTINGS = S3Settings(
            endpoint = URI.create("http://${RUSTFS.host}:${RUSTFS.getMappedPort(S3_PORT)}"),
            bucket = "drinkit-files",
            accessKey = "drinkit",
            secretKey = "drinkit-secret",
        )

        init {
            val credentials = AwsBasicCredentials.create(SETTINGS.accessKey, SETTINGS.secretKey)

            S3Client.builder()
                .endpointOverride(SETTINGS.endpoint)
                .region(Region.of(SETTINGS.region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build()
                .use { client -> client.createBucket { it.bucket(SETTINGS.bucket) } }
        }
    }
}
