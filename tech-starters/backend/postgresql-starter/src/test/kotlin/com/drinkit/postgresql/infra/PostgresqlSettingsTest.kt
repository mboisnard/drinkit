package com.drinkit.postgresql.infra

import com.drinkit.postgresql.JooqIntegrationTest
import io.kotest.assertions.throwables.shouldThrow
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType.VARCHAR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@JooqIntegrationTest
internal class PostgresqlSettingsTest {

    private val book = table(name("book"))
    private val title = field(name("title"), String::class.java)

    @BeforeEach
    fun createTable(dsl: DSLContext) {
        dsl.createTable(book).column(title, VARCHAR(50)).execute()
    }

    @Test
    fun `refuses an update that would change every row`(dsl: DSLContext) {
        shouldThrow<DataAccessException> { dsl.update(book).set(title, "Dune").execute() }
    }

    @Test
    fun `refuses a delete that would remove every row`(dsl: DSLContext) {
        shouldThrow<DataAccessException> { dsl.deleteFrom(book).execute() }
    }
}
