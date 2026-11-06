package com.drinkit.user

import com.drinkit.generated.jooq.DrinkitApplication
import com.drinkit.generated.jooq.tables.references.USER
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import com.drinkit.user.spi.Users
import com.drinkit.user.spi.UsersTestContract
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.string.shouldContain
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqUsersTest : UsersTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    override fun fetchRepository(): Users = JooqUsers(dsl = dsl, clock = ControlledClock())

    @Test
    fun `refuses a role the application does not define, even next to a known one`() {
        // When
        val refusal = shouldThrowAny { insertAUserWithRoles("ROLE_USER", "ROLE_ROOT") }

        // Then
        refusal.message shouldContain "roles_check"
    }

    @Test
    fun `refuses a user without any role`() {
        // When
        val refusal = shouldThrowAny { insertAUserWithRoles() }

        // Then
        refusal.message shouldContain "roles_check"
    }

    private fun insertAUserWithRoles(vararg roles: String) = dsl.insertInto(USER)
        .set(USER.ID, "000000000000000000000001")
        .set(USER.EMAIL, "jane.doe@example.com")
        .set(USER.STATUS, "ACTIVE")
        .set(USER.ENABLED, true)
        .set(USER.ROLES, arrayOf<String?>(*roles))
        .set(USER.MODIFIED, OffsetDateTime.parse("2026-11-06T10:00:00Z"))
        .execute()
}
