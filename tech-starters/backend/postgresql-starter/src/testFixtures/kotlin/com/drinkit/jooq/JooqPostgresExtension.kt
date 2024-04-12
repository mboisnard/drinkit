package com.drinkit.jooq

import com.drinkit.utils.orNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.DDLExportConfiguration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.Schema
import org.jooq.conf.Settings
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.junit.jupiter.api.extension.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.TestcontainersConfiguration
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

private const val PG_IMAGE_NAME = "postgres:16.2"
private const val DB_NAME = "TEST_DB"

class JooqPostgresExtension : BeforeAllCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

    private val logger = KotlinLogging.logger { }

    private lateinit var dbName: String
    private lateinit var connection: Connection
    private lateinit var dslContext: DSLContext

    private val container = PostgreSQLContainer(PG_IMAGE_NAME)
        .withDatabaseName(DB_NAME)
        .withStartupAttempts(2)
        .withReuse(true)

    private val ddlConfiguration = DDLExportConfiguration()
        .createSchemaIfNotExists(true)
        .createTableIfNotExists(true)
        .createIndexIfNotExists(true)
        .createDomainIfNotExists(true)
        .createSequenceIfNotExists(true)
        .createMaterializedViewIfNotExists(true)

    override fun beforeAll(extensionContext: ExtensionContext) {
        setReuseGlobalConfiguration()
        disableJooqNotRelevantLogs()

        if (!container.isRunning) {
            Startables.deepStart(container).join()
        }

        dbName = createARandomDbNameForTestClass()
        createADedicatedDatabaseForTestClass()

        connection = createConnection()
        dslContext = createJooqDslContext()

        createSchemasWithJooq(extensionContext)
        addSpringProperties()
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        connection.rollback()
    }

    override fun afterAll(extensionContext: ExtensionContext) {
        if (!container.isRunning) {
            return
        }

        dropDatabase()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        setOf(Connection::class.java, DSLContext::class.java).contains(parameterContext.parameter.type)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
        when (parameterContext.parameter.type) {
            Connection::class.java -> connection
            DSLContext::class.java -> dslContext
            else -> Unit
        }

    private fun setReuseGlobalConfiguration() {
        TestcontainersConfiguration.getInstance()
            .updateUserConfig("testcontainers.reuse.enable", "true")
    }

    private fun disableJooqNotRelevantLogs() {
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")
    }

    private fun createARandomDbNameForTestClass(): String = "db-${UUID.randomUUID()}"

    private fun createADedicatedDatabaseForTestClass() {
        container.createConnection("").use {
            it.createStatement().execute(
                "CREATE DATABASE \"$dbName\";"
            )
        }
    }

    private fun createConnection(): Connection {
        val connection = DriverManager.getConnection(
            containerJdbcUpdatedName(),
            container.username,
            container.password
        )
        connection.autoCommit = false
        return connection
    }

    private fun createJooqDslContext(): DSLContext {
        val jooqConfiguration = DefaultConfiguration()
            .set(connection)
            .set(SQLDialect.POSTGRES)
            .set(
                Settings()
                    .withExecuteWithOptimisticLocking(true)
                    .withExecuteWithOptimisticLockingExcludeUnversioned(true)
            )
        return DefaultDSLContext(jooqConfiguration)
    }

    private fun dropDatabase() {
        container.createConnection("").use {
            it.createStatement().execute(
                // Prevent new connections to the database
                "REVOKE CONNECT ON DATABASE \"$dbName\" FROM public;" +
                    // Kill existing connections to the database
                    "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '$dbName';"
            )
            it.createStatement().execute(
                "DROP DATABASE \"$dbName\""
            )
        }
    }

    private fun createSchemasWithJooq(ec: ExtensionContext) {
        val annotationClass = ec.testClass
            .map { it.getAnnotation(JooqIntegrationTest::class.java) }
            .filter { Objects.nonNull(it) }
            .filter { it.schemas.isNotEmpty() }
            .orNull() ?: return

        val queries = annotationClass.schemas
            .asSequence()
            .mapNotNull { it.companionObjectInstance }
            .map {
                val schemas = it.javaClass.kotlin.memberProperties
                    .filter { property -> property.visibility == KVisibility.PUBLIC }
                    .filter { property -> property.returnType.isSubtypeOf(Schema::class.createType()) }

                it to schemas
            }
            .flatMap { pair -> pair.second.map { it.call(pair.first) as Schema } }
            .map { dslContext.ddl(it, ddlConfiguration) }
            .toList()

        queries.flatMap { it.queries().toList() }.forEach {
            logger.debug { "Execute query: $it" }
            dslContext.execute(it)
            connection.commit()
        }
    }

    private fun containerJdbcUpdatedName() = container.jdbcUrl.replaceFirst(DB_NAME, dbName)

    private fun addSpringProperties() {
        System.setProperty("spring.datasource.url", containerJdbcUpdatedName())
        System.setProperty("spring.datasource.username", container.username)
        System.setProperty("spring.datasource.password", container.password)
    }

    /*@DynamicPropertySource
    fun registerPostgresqlProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { containerJdbcUpdatedName() }
        registry.add("spring.datasource.username") { container.username }
        registry.add("spring.datasource.password") { container.password }
    }*/
}
