package com.drinkit.postgresql

import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.string.shouldContain
import org.jooq.DSLContext
import org.jooq.impl.DSL.value
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@JooqIntegrationTest
@ExtendWith(OutputCaptureExtension::class)
internal class JooqPostgresExtensionTest {

    @Test
    fun `reports a query run again and again, as an N+1 does`(dsl: DSLContext, output: CapturedOutput) {
        listOf("Dune", "Emma", "Ulysses").forEach { dsl.select(value(it)).fetch() }

        output.out shouldContain "Repeated statements were encountered"
    }
}
