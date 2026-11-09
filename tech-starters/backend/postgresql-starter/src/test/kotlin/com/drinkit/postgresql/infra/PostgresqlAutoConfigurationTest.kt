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
            // When & Then
            val dsl = context.getBean<DSLContext>()
            dsl.dialect() shouldBe SQLDialect.POSTGRES
            dsl.settings().isExecuteWithOptimisticLocking shouldBe true
        }
    }

    @Test
    fun `lets the application replace the jOOQ settings of the starter`() {
        // Given
        val withOwnCustomizer = runner.withBean(
            STARTER_CUSTOMIZER,
            DefaultConfigurationCustomizer::class.java,
            { DefaultConfigurationCustomizer { } },
        )

        withOwnCustomizer.run { context ->
            // When & Then
            context.getBean<DSLContext>().settings().isExecuteWithOptimisticLocking shouldBe false
        }
    }

    @Test
    fun `turns a conflict on a versioned record into an optimistic locking failure`() {
        // Given
        val withConflictingShelves = runner
            .withConfiguration(AutoConfigurations.of(PersistenceExceptionTranslationAutoConfiguration::class.java))
            .withBean(ConflictingShelves::class.java)

        withConflictingShelves.run { context ->
            // When & Then
            shouldThrow<OptimisticLockingFailureException> { context.getBean<ConflictingShelves>().store() }
        }
    }

    @Test
    fun `watches the queries slower than the threshold of the application`() {
        // Given
        val withThreshold = runner.withPropertyValues("starters.postgresql.slow-query-threshold=2s")

        withThreshold.run { context ->
            // When
            val listeners = context.getBean<DSLContext>().configuration().executeListenerProviders()
                .map { it.provide() }
                .filterIsInstance<SlowQueryListener>()

            // Then
            listeners.map { it.threshold } shouldContainExactly listOf(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `checks no database before the application has one`() {
        // Given
        val withoutDatabase = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PostgresqlAutoConfiguration::class.java))

        withoutDatabase.run { context ->
            // When & Then
            context.startupFailure shouldBe null
            context.getBeanNamesForType(PostgresqlCheck::class.java).toList().shouldBeEmpty()
        }
    }

    @Test
    fun `registers nothing when disabled`() {
        // Given
        val disabled = runner.withPropertyValues("starters.postgresql.enabled=false")

        disabled.run { context ->
            // When & Then
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
