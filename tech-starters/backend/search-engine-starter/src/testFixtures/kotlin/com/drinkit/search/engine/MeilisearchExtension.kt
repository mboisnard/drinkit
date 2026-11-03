package com.drinkit.search.engine

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.Config
import io.vanslog.testcontainers.meilisearch.MeilisearchContainer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.TestcontainersConfiguration

// Keep in sync with deployment/local/compose.yml
private const val MEILISEARCH_IMAGE_NAME = "getmeili/meilisearch:v1.56.0"
private const val MASTER_KEY = "masterKey"

class MeilisearchExtension :
    BeforeAllCallback,
    AfterAllCallback,
    ParameterResolver {

    private lateinit var client: Client

    private val container = MeilisearchContainer(DockerImageName.parse(MEILISEARCH_IMAGE_NAME))
        .withMasterKey(MASTER_KEY)
        .withReuse(true)

    override fun beforeAll(extensionContext: ExtensionContext) {
        setReuseGlobalConfiguration()

        if (!container.isRunning) {
            Startables.deepStart(container).join()
        }

        client = createClient()
    }

    override fun afterAll(extensionContext: ExtensionContext) {
        client.indexes.results.forEach {
            client.deleteIndex(it.uid)
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type in setOf(Client::class.java, MeilisearchServer::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
        when (parameterContext.parameter.type) {
            Client::class.java -> client
            else -> MeilisearchServer(container.endpoint, MASTER_KEY)
        }

    private fun setReuseGlobalConfiguration() {
        TestcontainersConfiguration.getInstance()
            .updateUserConfig("testcontainers.reuse.enable", "true")
    }

    private fun createClient(): Client {
        val config = Config(container.endpoint, MASTER_KEY)
        return Client(config)
    }
}

data class MeilisearchServer(val url: String, val apiKey: String)
