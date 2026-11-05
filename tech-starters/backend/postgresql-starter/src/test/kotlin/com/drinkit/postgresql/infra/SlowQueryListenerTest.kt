package com.drinkit.postgresql.infra

import com.drinkit.postgresql.JooqIntegrationTest
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jooq.DSLContext
import org.jooq.impl.DSL.value
import org.jooq.impl.DefaultExecuteListenerProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import java.time.Duration

@JooqIntegrationTest
@ExtendWith(OutputCaptureExtension::class)
internal class SlowQueryListenerTest {

    @Test
    fun `logs a slow query with its SQL, never the values it binds`(dsl: DSLContext, output: CapturedOutput) {
        dsl.listenedFor(Duration.ofSeconds(2)).select(value("jane.doe@example.com")).fetch()

        output.out shouldContain "A query took 2000 ms, over the 1000 ms threshold: select ?"
        output.out shouldNotContain "jane.doe@example.com"
    }

    @Test
    fun `logs nothing for a query under the threshold`(dsl: DSLContext, output: CapturedOutput) {
        dsl.listenedFor(Duration.ofMillis(3)).select(value("jane.doe@example.com")).fetch()

        output.out shouldNotContain "A query took"
    }

    private fun DSLContext.listenedFor(duration: Duration): DSLContext {
        val ticks = ArrayDeque(listOf(0L, duration.toNanos()))
        val listener = SlowQueryListener(Duration.ofSeconds(1)) { ticks.removeFirst() }
        return configuration().derive(DefaultExecuteListenerProvider(listener)).dsl()
    }
}
