package com.drinkit.money.forex

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.money.forex.spi.ExchangeRates
import com.drinkit.money.forex.spi.ExchangeRatesTestContract
import com.drinkit.test.ControlledClock
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqExchangeRatesRepositoryTest : ExchangeRatesTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchRepository(): ExchangeRates = JooqExchangeRatesRepository(dsl = dsl, clock = ControlledClock())
}
