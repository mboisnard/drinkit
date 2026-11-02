package com.drinkit.postgresql

import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.jooq.ExecuteContext
import org.jooq.ExecuteListener
import org.jooq.Records.mapping
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table
import org.jooq.impl.DefaultExecuteListenerProvider
import org.jooq.impl.SQLDataType.INTEGER
import org.jooq.impl.SQLDataType.VARCHAR
import org.jooq.kotlin.mapping
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@JooqIntegrationTest
internal class MultisetTest {

    private val author = table(name("author"))
    private val authorId = field(name("author", "id"), Int::class.java)
    private val authorName = field(name("author", "name"), String::class.java)
    private val book = table(name("book"))
    private val bookAuthorId = field(name("book", "author_id"), Int::class.java)
    private val bookTitle = field(name("book", "title"), String::class.java)

    @BeforeEach
    fun fillLibrary(dsl: DSLContext) {
        dsl.createTable(author)
            .column(authorId.unqualifiedName, INTEGER)
            .column(authorName.unqualifiedName, VARCHAR(50))
            .execute()
        dsl.createTable(book)
            .column(bookAuthorId.unqualifiedName, INTEGER)
            .column(bookTitle.unqualifiedName, VARCHAR(50))
            .execute()
        listOf(1 to "Austen", 2 to "Herbert", 3 to "Joyce").forEach { (id, name) ->
            dsl.insertInto(author).set(authorId, id).set(authorName, name).execute()
        }
        listOf(1 to "Persuasion", 1 to "Emma", 2 to "Dune").forEach { (id, title) ->
            dsl.insertInto(book).set(bookAuthorId, id).set(bookTitle, title).execute()
        }
    }

    // MULTISET nests the books of each author in its row, in one query: a join repeats the author on each book and
    // drops the authors without one, and a query per author is an N+1
    @Test
    fun `reads every author with their books in a single query`(dsl: DSLContext) {
        val queries = QueryCounter()
        val books = multiset(select(bookTitle).from(book).where(bookAuthorId.eq(authorId)).orderBy(bookTitle))

        val authors = dsl.configuration().derive(DefaultExecuteListenerProvider(queries)).dsl()
            .select(authorName, books.mapping(::Book))
            .from(author)
            .orderBy(authorName)
            .fetch(mapping(::AuthorWithBooks))

        authors shouldContainExactly listOf(
            AuthorWithBooks("Austen", listOf(Book("Emma"), Book("Persuasion"))),
            AuthorWithBooks("Herbert", listOf(Book("Dune"))),
            AuthorWithBooks("Joyce", emptyList()),
        )
        queries.count shouldBe 1
    }

    private data class Book(val title: String)

    private data class AuthorWithBooks(val name: String, val books: List<Book>)

    private class QueryCounter : ExecuteListener {
        var count = 0

        override fun executeStart(ctx: ExecuteContext) {
            count++
        }
    }
}
