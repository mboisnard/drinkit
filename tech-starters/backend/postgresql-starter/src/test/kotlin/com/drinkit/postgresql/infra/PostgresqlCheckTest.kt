package com.drinkit.postgresql.infra

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class PostgresqlCheckTest {

    @Test
    fun `refuses to start on a database other than PostgreSQL`() {
        // When
        val failure = shouldThrow<IllegalStateException> { PostgresqlCheck { "H2" }.afterPropertiesSet() }

        // Then
        failure.message shouldContain "H2"
    }
}
