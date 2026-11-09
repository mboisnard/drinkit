package com.drinkit.postgresql.infra

import com.drinkit.postgresql.JooqIntegrationTest
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.ExecuteWithoutWhere
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import javax.sql.DataSource

// A real application, not a context runner: only it reads spring.factories and the auto-configuration imports
@JooqIntegrationTest
internal class PostgresqlStarterTest {

    @Test
    fun `gives the application a jOOQ DSLContext on PostgreSQL with the settings of the starter`() {
        application().use { context ->
            // When & Then
            val dsl = context.getBean<DSLContext>()
            dsl.dialect() shouldBe SQLDialect.POSTGRES
            dsl.settings().executeDeleteWithoutWhere shouldBe ExecuteWithoutWhere.THROW
        }
    }

    @Test
    fun `connects with auto-commit off, so that only the transaction of the use case commits`() {
        application().use { context ->
            // When & Then
            context.getBean<DataSource>().connection.use { it.autoCommit } shouldBe false
        }
    }

    @Test
    fun `bounds in time every wait of a session`() {
        application().use { context ->
            // When & Then
            val dataSource = context.getBean<DataSource>()
            dataSource.setting("statement_timeout") shouldBe "30s"
            dataSource.setting("lock_timeout") shouldBe "5s"
            dataSource.setting("idle_in_transaction_session_timeout") shouldBe "1min"
        }
    }

    @Test
    fun `names each session after the application`() {
        application().use { context ->
            // When & Then
            context.getBean<DataSource>().setting("application_name") shouldBe "inventory"
        }
    }

    @Test
    fun `lets the application replace a default of the starter`() {
        application(
            "--spring.datasource.hikari.data-source-properties.options=-c statement_timeout=10s",
        ).use { context ->
            // When & Then
            context.getBean<DataSource>().setting("statement_timeout") shouldBe "10s"
        }
    }

    private fun application(vararg arguments: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(Application::class.java)
            .web(WebApplicationType.NONE)
            .properties("spring.application.name=inventory")
            .run(*arguments)

    private fun DataSource.setting(name: String): String = connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SHOW $name").use { result ->
                result.next()
                result.getString(1)
            }
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    class Application
}
