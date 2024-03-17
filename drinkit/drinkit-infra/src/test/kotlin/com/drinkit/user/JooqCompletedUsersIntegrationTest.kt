package com.drinkit.user

import com.drinkit.generated.jooq.Public
import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

@JooqIntegrationTest(schemas = [Public::class])
internal class JooqCompletedUsersIntegrationTest {

    @Test
    fun `should start postgresql`() {
        true shouldBe true
    }
}
