package com.drinkit.user

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import com.drinkit.user.spi.Users
import com.drinkit.user.spi.UsersTestContract
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqUsersTest : UsersTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchRepository(): Users = JooqUsers(dsl = dsl, clock = ControlledClock())
}
