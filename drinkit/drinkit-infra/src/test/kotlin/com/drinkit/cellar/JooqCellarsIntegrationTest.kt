package com.drinkit.cellar

import com.drinkit.cellar.spi.Cellars
import com.drinkit.cellar.spi.CellarsTestContract
import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import tools.jackson.databind.json.JsonMapper

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
        JooqCellars(dslContext, cellarFixtures.controlledClock, JsonMapper.builder().build())
}
