package com.drinkit.postgresql

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL.constraint
import org.jooq.impl.DSL.name
import org.jooq.impl.SQLDataType.VARCHAR
import org.jooq.impl.TableImpl
import org.jooq.impl.TableRecordImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessException

@JooqIntegrationTest
internal class JooqUtilsTest {

    @BeforeEach
    fun fillTable(dsl: DSLContext) {
        dsl.createTable(Books).columns(Books.TITLE, Books.AUTHOR)
            .constraint(constraint("book_pkey").primaryKey(Books.TITLE))
            .execute()
        listOf("Dune" to "Herbert", "Emma" to "Austen", "Ulysses" to "Joyce").forEach { (title, author) ->
            dsl.insertInto(Books).set(Books.TITLE, title).set(Books.AUTHOR, author).execute()
        }
    }

    @Test
    fun `reads every field of a table as its record`(dsl: DSLContext) {
        // When
        val books = dsl.select(allFields(Books)).from(Books).orderBy(Books.TITLE)
            .fetch { it.value1()[Books.TITLE] to it.value1()[Books.AUTHOR] }

        // Then
        books shouldContainExactly listOf("Dune" to "Herbert", "Emma" to "Austen", "Ulysses" to "Joyce")
    }

    @Test
    fun `maps the rows of a sequence only as the caller reads them`(dsl: DSLContext) {
        // Given
        val mapped = mutableListOf<String?>()
        val titles = dsl.select(Books.TITLE).from(Books).orderBy(Books.TITLE)
            .fetchSequence { it.value1().also(mapped::add) }

        // When
        val firstTwo = titles.take(2).toList()

        // Then
        firstTwo shouldContainExactly listOf("Dune", "Emma")
        mapped.size shouldBe 2
    }

    @Test
    fun `tells that a duplicate key violates the primary key`(dsl: DSLContext) {
        // When
        val refusal = shouldThrow<DataAccessException> { dsl.insertInto(Books).set(Books.TITLE, "Dune").execute() }

        // Then
        refusal.violates("book_pkey") shouldBe true
    }

    @Test
    fun `tells that a duplicate key violates no other constraint`(dsl: DSLContext) {
        // When
        val refusal = shouldThrow<DataAccessException> { dsl.insertInto(Books).set(Books.TITLE, "Dune").execute() }

        // Then
        refusal.violates("author_pkey") shouldBe false
    }

    // A table and its record, as jOOQ generates them
    private object Books : TableImpl<BookRecord>(name("book")) {
        val TITLE: TableField<BookRecord, String?> = createField(name("title"), VARCHAR(50), this)
        val AUTHOR: TableField<BookRecord, String?> = createField(name("author"), VARCHAR(50), this)

        override fun getRecordType() = BookRecord::class.java
    }

    private class BookRecord : TableRecordImpl<BookRecord>(Books)
}
