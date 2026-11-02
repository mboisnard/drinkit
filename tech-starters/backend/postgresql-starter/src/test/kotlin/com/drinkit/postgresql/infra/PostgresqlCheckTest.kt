package com.drinkit.postgresql.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class PostgresqlCheckTest {

    @Test
    fun `refuses to start on a database other than PostgreSQL`() {
        val failure = shouldThrow<IllegalStateException> { PostgresqlCheck { "H2" }.afterPropertiesSet() }

        failure.message shouldContain "H2"
    }
}
