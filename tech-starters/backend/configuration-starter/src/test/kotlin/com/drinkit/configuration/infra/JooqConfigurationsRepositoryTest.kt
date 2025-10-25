package com.drinkit.configuration.infra

import com.drinkit.configuration.Configurations
import com.drinkit.configuration.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import java.time.Clock

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
            objectMapper = jacksonObjectMapper(),
            clock = Clock.systemUTC() //TODO Use ControlledClock (move to test-starter)
        )
    }
}
