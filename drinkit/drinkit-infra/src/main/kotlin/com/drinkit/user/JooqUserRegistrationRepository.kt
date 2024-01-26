package com.drinkit.user

import com.drinkit.generated.jooq.tables.references.USER
import com.drinkit.user.registration.NotCompletedUser
import com.drinkit.user.registration.UserRegistrationRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime

@Repository
internal class JooqUserRegistrationRepository(
    private val dslContext: DSLContext,
    private val clock: Clock,
): UserRegistrationRepository {

    override fun emailExists(email: Email): Boolean {
        val count = dslContext.fetchCount(USER, USER.EMAIL.eq(email.value))
        return count != 0
    }

    override fun create(user: NotCompletedUser): UserId? {
        val query = dslContext.insertInto(USER)
            .set(USER.ID, user.id.value)
            .set(USER.EMAIL, user.email.value)
            .set(USER.PASSWORD, user.password.value)
            .set(USER.ENABLED, user.enabled)
            .set(USER.MODIFIED, LocalDateTime.now(clock))
            .onConflict(USER.ID)
            .doNothing()

        val insertRowCount = query.execute()

        return if (insertRowCount != 0) user.id else null
    }

    override fun update(user: NotCompletedUser) {
        TODO("Not yet implemented")
    }
}