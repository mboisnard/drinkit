package com.drinkit.postgresql.infra

import com.drinkit.postgresql.JooqIntegrationTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.exception.DataChangedException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.PersistenceExceptionTranslationAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository
import java.time.Duration

@JooqIntegrationTest
internal class PostgresqlAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                JooqAutoConfiguration::class.java,
                PostgresqlAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `talks to PostgreSQL with optimistic locking`() {
        runner.run { context ->
            val dsl = context.getBean<DSLContext>()
            dsl.dialect() shouldBe SQLDialect.POSTGRES
            dsl.settings().isExecuteWithOptimisticLocking shouldBe true
        }
    }

    @Test
    fun `lets the application replace the jOOQ settings of the starter`() {
        runner.withBean(
            STARTER_CUSTOMIZER,
            DefaultConfigurationCustomizer::class.java,
            { DefaultConfigurationCustomizer { } },
        )
            .run { context ->
                context.getBean<DSLContext>().settings().isExecuteWithOptimisticLocking shouldBe false
            }
    }

    @Test
    fun `turns a conflict on a versioned record into an optimistic locking failure`() {
        runner.withConfiguration(AutoConfigurations.of(PersistenceExceptionTranslationAutoConfiguration::class.java))
            .withBean(ConflictingShelves::class.java)
            .run { context ->
                shouldThrow<OptimisticLockingFailureException> { context.getBean<ConflictingShelves>().store() }
            }
    }

    @Test
    fun `watches the queries slower than the threshold of the application`() {
        runner.withPropertyValues("starters.postgresql.slow-query-threshold=2s")
            .run { context ->
                val listeners = context.getBean<DSLContext>().configuration().executeListenerProviders()
                    .map { it.provide() }
                    .filterIsInstance<SlowQueryListener>()
                listeners.map { it.threshold } shouldContainExactly listOf(Duration.ofSeconds(2))
            }
    }

    @Test
    fun `registers nothing when disabled`() {
        runner.withPropertyValues("starters.postgresql.enabled=false")
            .run { context ->
                context.getBeanNamesForType(PostgresqlAutoConfiguration::class.java).toList().shouldBeEmpty()
                context.containsBean(STARTER_CUSTOMIZER) shouldBe false
            }
    }

    @Repository
    class ConflictingShelves {
        fun store(): Unit = throw DataChangedException("Database record has been changed")
    }

    companion object {
        private const val STARTER_CUSTOMIZER = "postgresqlJooqCustomizer"
    }
}
