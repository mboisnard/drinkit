package com.drinkit.user

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.UserEventsTestContract
import com.drinkit.user.spi.Users
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqUserEventsRepositoryTest : UserEventsTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchUsers(): Users = JooqUsersRepository(dsl = dsl, clock = ControlledClock())

    override fun fetchUserEvents(users: Users): UserEvents {
        val jsonMapper = JsonMapper.builder()
            .addModule(kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) })
            .addModule(UserEventMixinConfiguration().authorModule())
            .build()

        return JooqUserEventsRepository(
            dsl = dsl,
            jsonMapper = jsonMapper,
            users = users,
        )
    }
}