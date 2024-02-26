package com.drinkit.user

import com.drinkit.generated.jooq.Public
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.user.registration.NotCompletedUsers
import com.drinkit.user.registration.NotCompletedCompletedUsersContract
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import java.time.Clock

@JooqIntegrationTest(schemas = [Public::class])
internal class JooqNotCompletedCompletedUsersIntegrationTest: NotCompletedCompletedUsersContract() {

    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun setup(dslContext: DSLContext) {
        this.dslContext = dslContext
    }

    override fun fetchRepository(): NotCompletedUsers =
        JooqNotCompletedUsers(dslContext, Clock.systemDefaultZone())
}