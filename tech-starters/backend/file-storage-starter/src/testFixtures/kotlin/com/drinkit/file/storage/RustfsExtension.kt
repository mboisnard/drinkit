package com.drinkit.file.storage

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.TestcontainersConfiguration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.HttpURLConnection.HTTP_OK
import java.net.URI

private const val RUSTFS_IMAGE_NAME = "rustfs/rustfs:1.0.4"
private const val S3_PORT = 9000
private const val ACCESS_KEY = "drinkit"
private const val SECRET_KEY = "drinkit-secret"
private const val BUCKET = "drinkit-files"

/** An S3-compatible server that checks the signatures, as a bucket in the cloud does. */
@ExtendWith(RustfsExtension::class)
annotation class RustfsIntegrationTest

data class S3Server(val endpoint: URI, val accessKey: String, val secretKey: String, val bucket: String)

class RustfsExtension :
    BeforeAllCallback,
    ParameterResolver {

    private val container = GenericContainer(DockerImageName.parse(RUSTFS_IMAGE_NAME))
        .withExposedPorts(S3_PORT)
        .withEnv("RUSTFS_ACCESS_KEY", ACCESS_KEY)
        .withEnv("RUSTFS_SECRET_KEY", SECRET_KEY)
        .waitingFor(Wait.forHttp("/health").forStatusCode(HTTP_OK))
        .withReuse(true)

    private lateinit var server: S3Server

    override fun beforeAll(extensionContext: ExtensionContext) {
        TestcontainersConfiguration.getInstance().updateUserConfig("testcontainers.reuse.enable", "true")

        if (!container.isRunning) {
            Startables.deepStart(container).join()
        }

        server = S3Server(
            URI.create("http://${container.host}:${container.getMappedPort(S3_PORT)}"),
            ACCESS_KEY,
            SECRET_KEY,
            BUCKET,
        )
        createTheBucket()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == S3Server::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any = server

    private fun createTheBucket() {
        S3Client.builder()
            .endpointOverride(server.endpoint)
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
            .forcePathStyle(true)
            .build()
            .use { client ->
                if (client.listBuckets().buckets().none { it.name() == BUCKET }) {
                    client.createBucket { it.bucket(BUCKET) }
                }
            }
    }
}
