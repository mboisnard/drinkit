package com.drinkit.idempotency.infra

import com.drinkit.idempotency.Idempotency
import com.drinkit.idempotency.IdempotentResult
import com.drinkit.idempotency.generated.jooq.DrinkitApplication
import com.drinkit.idempotency.inMemoryIdempotency
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class IdempotencyAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                DataSourceTransactionManagerAutoConfiguration::class.java,
                TransactionAutoConfiguration::class.java,
                JooqAutoConfiguration::class.java,
                IdempotencyAutoConfiguration::class.java,
            ),
        )
        .withBean(Clock::class.java, { ControlledClock() })

    @Test
    fun `shows a key taken inside a transaction to the other instances before the transaction ends`() {
        runner.run { context ->
            val idempotency = context.getBean<Idempotency>()
            val key = UUID.randomUUID().toString()

            // When
            val otherInstance = TransactionTemplate(context.getBean<PlatformTransactionManager>()).execute {
                idempotency.once("welcome-email", key) {
                    CompletableFuture.supplyAsync { idempotency.once("welcome-email", key) { "sent twice" } }
                        .get(5, TimeUnit.SECONDS)
                }
            }

            // Then
            otherInstance.shouldBeInstanceOf<IdempotentResult.Executed<*>>().value
                .shouldBeInstanceOf<IdempotentResult.InFlight>()
        }
    }

    @Test
    fun `steps aside for the application's own idempotency`() {
        val own = inMemoryIdempotency()

        runner.withBean(Idempotency::class.java, { own }).run { context ->
            context.getBean<Idempotency>() shouldBe own
        }
    }

    @Test
    fun `registers nothing when switched off`() {
        runner.withPropertyValues("starters.idempotency.enabled=false").run { context ->
            context.getBeanNamesForType(Idempotency::class.java).size shouldBe 0
        }
    }
}
