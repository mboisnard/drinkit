package com.drinkit.configuration.infra

import com.drinkit.configuration.Configurations
import com.drinkit.configuration.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import tools.jackson.module.kotlin.jacksonMapperBuilder
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqConfigurationsRepositoryTest : ConfigurationsTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchRepository(): Configurations {
        return JooqConfigurationsRepository(
            dsl = dsl,
            jsonMapper = jacksonMapperBuilder().build(),
            clock = ControlledClock()
        )
    }
}
