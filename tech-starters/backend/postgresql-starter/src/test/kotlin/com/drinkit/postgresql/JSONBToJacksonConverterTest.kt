package com.drinkit.postgresql

import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.math.BigDecimal

@JooqIntegrationTest
internal class JSONBToJacksonConverterTest {

    private val mapper = JsonMapper.builder().addModule(kotlinModule()).build()
    private val document = table(name("document"))
    private val details = field(name("details"), SQLDataType.JSONB)

    @BeforeEach
    fun createTable(dsl: DSLContext) {
        dsl.createTable(document).column(details).execute()
    }

    @Test
    fun `drops the null character that PostgreSQL refuses in a JSONB text`(dsl: DSLContext) {
        dsl.roundTrip(Note("Pasted\u0000 text")) shouldBe Note("Pasted text")
    }

    @Test
    fun `keeps a backslash followed by u0000 written in the text`(dsl: DSLContext) {
        dsl.roundTrip(Note("""C:\u0000""")) shouldBe Note("""C:\u0000""")
    }

    @Test
    fun `brings numbers back unchanged`(dsl: DSLContext) {
        val amounts = Amounts(
            price = "12345678901234567890.50".toBigDecimal(),
            count = Long.MAX_VALUE,
            ratio = 13.5,
        )

        dsl.roundTrip(amounts) shouldBe amounts
    }

    private inline fun <reified T : Any> DSLContext.roundTrip(value: T): T {
        val converter = JSONBToJacksonConverter(T::class.java, mapper)
        insertInto(document).set(details, converter.to(value)).execute()
        return converter.from(select(details).from(document).fetchSingle().value1() as JSONB)
    }

    data class Note(val text: String)

    data class Amounts(val price: BigDecimal, val count: Long, val ratio: Double)
}
