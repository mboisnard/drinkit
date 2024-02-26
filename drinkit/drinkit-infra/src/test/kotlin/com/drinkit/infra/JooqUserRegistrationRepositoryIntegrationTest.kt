package com.drinkit.infra

import com.drinkit.generated.jooq.Public
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.user.JooqUserRegistrationRepository
import com.drinkit.user.registration.UserRegistrationRepository
import com.drinkit.user.registration.UserRegistrationRepositoryContract
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import java.time.Clock

@JooqIntegrationTest(schemas = [Public::class])
internal class JooqUserRegistrationRepositoryIntegrationTest: UserRegistrationRepositoryContract() {

    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun setup(dslContext: DSLContext) {
        this.dslContext = dslContext
    }

    override fun fetchRepository(): UserRegistrationRepository =
        JooqUserRegistrationRepository(dslContext, Clock.systemDefaultZone())
}