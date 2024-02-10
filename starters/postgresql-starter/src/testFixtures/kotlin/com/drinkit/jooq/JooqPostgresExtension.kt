package com.drinkit.jooq

import com.drinkit.utils.orNull
import org.jooq.DDLExportConfiguration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.Schema
import org.jooq.conf.Settings
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.junit.jupiter.api.extension.*
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.TestcontainersConfiguration
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties


private const val PG_IMAGE_NAME = "postgres:16.1"
private const val DB_NAME = "TEST_DB"

class JooqPostgresExtension: BeforeAllCallback, AfterAllCallback, ParameterResolver {

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

    override fun beforeAll(ec: ExtensionContext) {
        setReuseGlobalConfiguration()

        if (!container.isRunning)
            Startables.deepStart(container).join()

        dbName = createARandomDbNameForTestClass()
        createADedicatedDatabaseForTestClass()

        connection = createConnection()
        dslContext = createJooqDslContext()

        createSchemasWithJooq(ec)
    }

    override fun afterAll(ex: ExtensionContext) {
        if (!container.isRunning)
            return

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

    private fun createARandomDbNameForTestClass(): String = "db-${UUID.randomUUID()}"

    private fun createADedicatedDatabaseForTestClass() {
        container.createConnection("").use {
            it.createStatement().execute(
                "CREATE DATABASE \"$dbName\";"
            )
        }
    }

    private fun createConnection(): Connection = DriverManager.getConnection(
        containerJdbcUpdatedName(),
        container.username,
        container.password
    )

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
            .flatMap { it.memberProperties }
            .filter { it.visibility == KVisibility.PUBLIC }
            .filterIsInstance<KProperty<Schema>>()
            //.map { it.call() }
            //.map { dslContext.ddl(it, ddlConfiguration) }
            .toList()

        println(annotationClass)
    }

    private fun containerJdbcUpdatedName() = container.jdbcUrl.replaceFirst(DB_NAME, dbName)

    @DynamicPropertySource
    fun registerPostgresqlProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { containerJdbcUpdatedName() }
        registry.add("spring.datasource.username") { container.username }
        registry.add("spring.datasource.password") { container.password }
    }
}
