package com.drinkit.configuration.infra

import com.drinkit.cache.CacheTtl
import com.drinkit.cache.ControlledTicker
import com.drinkit.configuration.Configurations
import com.drinkit.configuration.InMemoryConfigurations
import com.drinkit.configuration.configurationKey
import com.drinkit.configuration.generated.jooq.DrinkitApplication
import com.drinkit.configuration.generated.jooq.tables.references.CONFIGURATION
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import com.github.benmanes.caffeine.cache.Ticker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL.currentOffsetDateTime
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Clock
import java.time.Duration

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class ConfigurationAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                DataSourceTransactionManagerAutoConfiguration::class.java,
                TransactionAutoConfiguration::class.java,
                JooqAutoConfiguration::class.java,
                // Internal to cache-starter: found by its name, as its AutoConfiguration.imports file names it
                Class.forName("com.drinkit.cache.infra.CacheStarterAutoConfiguration"),
                CacheAutoConfiguration::class.java,
                ConfigurationAutoConfiguration::class.java,
            ),
        )
        .withBean(JsonMapper::class.java, { jacksonMapperBuilder().build() })
        .withBean(Clock::class.java, { ControlledClock() })

    private val retries = configurationKey("test.retries", default = 3)

    @Test
    fun `reads the configurations from PostgreSQL`() {
        runner.run { context ->
            // When & Then
            context.getBean<Configurations>().shouldBeInstanceOf<JooqConfigurations>()
        }
    }

    @Test
    fun `serves a value it read from its cache`() {
        runner.run { context ->
            // Given
            val configurations = context.getBean<Configurations>()
            configurations[retries] shouldBe 3
            anotherInstanceStores(context, "5")

            try {
                // When
                val cached = configurations[retries]

                // Then
                cached shouldBe 3
            } finally {
                inTransaction(context) { configurations.reset(retries) }
            }
        }
    }

    @Test
    fun `reads again, once its cache entry is 10 minutes old, a value another instance changed`() {
        // Given
        val ticker = ControlledTicker()
        val withTicker = runner.withBean(Ticker::class.java, { ticker })

        withTicker.run { context ->
            val configurations = context.getBean<Configurations>()
            configurations[retries] shouldBe 3
            anotherInstanceStores(context, "5")

            try {
                // When
                ticker.advance(Duration.ofMinutes(10).minusSeconds(1))
                val beforeExpiry = configurations[retries]
                ticker.advance(Duration.ofSeconds(2))
                val afterExpiry = configurations[retries]

                // Then
                beforeExpiry shouldBe 3
                afterExpiry shouldBe 5
            } finally {
                inTransaction(context) { configurations.reset(retries) }
            }
        }
    }

    @Test
    fun `reads at once the value it set`() {
        runner.run { context ->
            // Given
            val configurations = context.getBean<Configurations>()
            configurations[retries] shouldBe 3

            try {
                // When
                inTransaction(context) { configurations[retries] = 5 }

                // Then
                configurations[retries] shouldBe 5
            } finally {
                inTransaction(context) { configurations.reset(retries) }
            }
        }
    }

    @Test
    fun `never reads a value set in a transaction that rolls back`() {
        runner.run { context ->
            // Given
            val configurations = context.getBean<Configurations>()

            // When
            context.getBean<TransactionTemplate>().executeWithoutResult { status ->
                configurations[retries] = 5
                status.setRollbackOnly()
            }

            // Then
            configurations[retries] shouldBe 3
        }
    }

    @Test
    fun `never reads a value it read inside a transaction that rolls back`() {
        runner.run { context ->
            // Given
            val configurations = context.getBean<Configurations>()

            // When
            context.getBean<TransactionTemplate>().executeWithoutResult { status ->
                configurations[retries] = 5
                configurations[retries]
                status.setRollbackOnly()
            }

            // Then
            configurations[retries] shouldBe 3
        }
    }

    @Test
    fun `refuses a value set outside a transaction`() {
        runner.run { context ->
            // When & Then
            shouldThrow<IllegalTransactionStateException> { context.getBean<Configurations>()[retries] = 5 }
        }
    }

    @Test
    fun `lets the application replace the time to live of its cache`() {
        // Given
        val withOwnTtl = runner.withBean(
            "configurationsCacheTtl",
            CacheTtl::class.java,
            { CacheTtl("configurations", Duration.ofSeconds(5)) },
        )

        withOwnTtl.run { context ->
            // When & Then
            context.getBeansOfType(CacheTtl::class.java).values.toList() shouldBe
                listOf(CacheTtl("configurations", Duration.ofSeconds(5)))
        }
    }

    @Test
    fun `backs off when the application declares its own configurations`() {
        // Given
        val withOwnConfigurations = runner.withBean(Configurations::class.java, { InMemoryConfigurations() })

        withOwnConfigurations.run { context ->
            // When & Then
            context.getBean<Configurations>().shouldBeInstanceOf<InMemoryConfigurations>()
        }
    }

    @Test
    fun `registers no configurations without a database`() {
        // Given
        val withoutDatabase = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationAutoConfiguration::class.java))
            .withBean(JsonMapper::class.java, { jacksonMapperBuilder().build() })
            .withBean(Clock::class.java, { ControlledClock() })

        withoutDatabase.run { context ->
            // When & Then
            context.startupFailure shouldBe null
            context.getBeanNamesForType(Configurations::class.java).toList().shouldBeEmpty()
        }
    }

    @Test
    fun `registers nothing when disabled`() {
        // Given
        val disabled = runner.withPropertyValues("starters.configuration.enabled=false")

        disabled.run { context ->
            // When & Then
            context.getBeanNamesForType(Configurations::class.java).toList().shouldBeEmpty()
        }
    }

    private fun inTransaction(context: AssertableApplicationContext, action: () -> Unit) =
        context.getBean<TransactionTemplate>().executeWithoutResult { action() }

    private fun anotherInstanceStores(context: AssertableApplicationContext, json: String) = inTransaction(context) {
        context.getBean<DSLContext>().insertInto(CONFIGURATION)
            .set(CONFIGURATION.KEY, retries.name)
            .set(CONFIGURATION.VALUE, JSONB.jsonb(json))
            .set(CONFIGURATION.MODIFIED, currentOffsetDateTime())
            .execute()
    }
}
