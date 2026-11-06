package com.drinkit.mail

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.testcontainers.containers.GenericContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.TestcontainersConfiguration

// Not the community module ch.martinelli.oss:testcontainers-mailpit: it brings Jackson 2 and an auto-configuration
private const val MAILPIT_IMAGE_NAME = "axllent/mailpit:v1.32.0"
private const val SMTP_PORT = 1025
private const val API_PORT = 8025

class MailpitExtension :
    BeforeAllCallback,
    BeforeEachCallback,
    AfterAllCallback,
    ParameterResolver {

    private lateinit var mailpit: Mailpit

    private val container = GenericContainer(DockerImageName.parse(MAILPIT_IMAGE_NAME))
        .withExposedPorts(SMTP_PORT, API_PORT)
        .withEnv("MP_ENABLE_CHAOS", "true")
        .withReuse(true)

    override fun beforeAll(extensionContext: ExtensionContext) {
        setReuseGlobalConfiguration()

        if (!container.isRunning) {
            Startables.deepStart(container).join()
        }

        mailpit = Mailpit(container.host, container.getMappedPort(SMTP_PORT), container.getMappedPort(API_PORT))
    }

    // The container outlives the run, so a run stopped while refusing would leave it refusing
    override fun beforeEach(extensionContext: ExtensionContext) = mailpit.acceptAll()

    override fun afterAll(extensionContext: ExtensionContext) {
        mailpit.deleteEmails()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == Mailpit::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any = mailpit

    private fun setReuseGlobalConfiguration() {
        TestcontainersConfiguration.getInstance()
            .updateUserConfig("testcontainers.reuse.enable", "true")
    }
}
