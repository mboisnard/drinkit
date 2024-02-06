package com.drinkit.infra

import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.junit.jupiter.api.Test

@JooqIntegrationTest
internal class UserRepositoryIntegrationTest {

    @Test
    fun `should start postgresql`(dslContext: DSLContext) {
        true shouldBe true
    }
}