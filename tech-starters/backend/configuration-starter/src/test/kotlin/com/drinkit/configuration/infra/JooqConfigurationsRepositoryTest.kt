package com.drinkit.configuration.infra

import com.drinkit.configuration.Configurations
import com.drinkit.configuration.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import tools.jackson.module.kotlin.jacksonMapperBuilder

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqConfigurationsRepositoryTest : ConfigurationsTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchRepository(): Configurations = JooqConfigurationsRepository(
        dsl = dsl,
        jsonMapper = jacksonMapperBuilder().build(),
        clock = ControlledClock(),
    )
}
