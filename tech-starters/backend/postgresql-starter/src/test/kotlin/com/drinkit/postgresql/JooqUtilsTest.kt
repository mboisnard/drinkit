package com.drinkit.postgresql

import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType.VARCHAR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@JooqIntegrationTest
internal class JooqUtilsTest {

    private val book = table(name("book"))
    private val title = field(name("title"), String::class.java)

    @BeforeEach
    fun fillTable(dsl: DSLContext) {
        dsl.createTable(book).column(title, VARCHAR(50)).execute()
        listOf("Dune", "Emma", "Ulysses").forEach { dsl.insertInto(book).set(title, it).execute() }
    }

    @Test
    fun `maps the rows of a sequence only as the caller reads them`(dsl: DSLContext) {
        val mapped = mutableListOf<String>()
        val titles = dsl.select(title).from(book).orderBy(title).fetchSequence { it.value1().also(mapped::add) }

        titles.take(2).toList() shouldContainExactly listOf("Dune", "Emma")
        mapped.size shouldBe 2
    }
}
