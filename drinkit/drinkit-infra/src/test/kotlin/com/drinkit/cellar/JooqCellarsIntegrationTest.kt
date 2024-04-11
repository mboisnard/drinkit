package com.drinkit.cellar

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqCellarsIntegrationTest : CellarsTestContract() {

    private lateinit var dslContext: DSLContext
    private lateinit var cellarFixtures: CellarFixtures

    @BeforeEach
    fun setup(dslContext: DSLContext) {
        this.dslContext = dslContext
        this.cellarFixtures = CellarFixtures()
    }

    override fun fetchRepository(): Cellars =
        JooqCellars(dslContext, cellarFixtures.controlledClock, ObjectMapper())
}
