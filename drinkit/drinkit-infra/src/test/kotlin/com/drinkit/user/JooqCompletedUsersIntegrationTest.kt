package com.drinkit.user

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.jooq.JooqIntegrationTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqCompletedUsersIntegrationTest {

    @Test
    fun `should start postgresql`() {
        true shouldBe true
    }
}
