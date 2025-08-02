package com.drinkit.user

import com.drinkit.common.ControlledClock
import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.UserEventsTestContract
import com.drinkit.user.spi.Users
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqUserEventsRepositoryTest : UserEventsTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchUsers(): Users = JooqUsersRepository(dsl = dsl, clock = ControlledClock())

    override fun fetchUserEvents(users: Users): UserEvents {
        val configuration = UserEventMixinConfiguration()


        val objectMapper = JsonMapper.builder()
            .addModule(kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) })
            .addModule(configuration.authorModule())
            .build()

        return JooqUserEventsRepository(
            dsl = dsl,
            objectMapper = objectMapper,
            users = users,
        )
    }
}