package com.drinkit.user

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.user.registration.NotCompletedCompletedUsersTestContract
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqNotCompletedCompletedUsersIntegrationTest : NotCompletedCompletedUsersTestContract() {

    private lateinit var dslContext: DSLContext
    private lateinit var userFixtures: UserFixtures

    @BeforeEach
    fun setup(dslContext: DSLContext) {
        this.dslContext = dslContext
        this.userFixtures = UserFixtures()
    }

    override fun fetchRepository(): NotCompletedUsers =
        JooqNotCompletedUsers(dslContext, userFixtures.controlledClock)
}
