package com.drinkit.user

import com.drinkit.generated.jooq.tables.User.Companion.USER
import com.drinkit.generated.jooq.tables.records.UserRecord
import com.drinkit.jooq.allFields
import com.drinkit.user.registration.NotCompletedUsers
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime

@Repository
internal class JooqNotCompletedUsers(
    private val dslContext: DSLContext,
    private val clock: Clock,
) : NotCompletedUsers {

    override fun emailExists(email: Email): Boolean {
        val count = dslContext.fetchCount(USER, USER.EMAIL.eq(email.value))
        return count != 0
    }

    override fun findById(userId: UserId): NotCompletedUser? {
        val query = dslContext.select(allFields(USER))
            .from(USER)
            .where(
                USER.ID.eq(userId.value)
                    .and(USER.COMPLETED.eq(false))
                    .and(USER.ENABLED.eq(true))
            )

        return query.fetchOne { it.value1() }?.toNotCompletedUser()
    }

    override fun create(user: NotCompletedUser): UserId? {
        val query = dslContext.insertInto(USER)
            .set(USER.ID, user.id.value)
            .set(USER.EMAIL, user.email.value)
            .set(USER.PASSWORD, user.password!!.value)
            .set(USER.STATUS, user.status)
            .set(USER.COMPLETED, false)
            .set(USER.ENABLED, user.enabled)
            .set(USER.MODIFIED, LocalDateTime.now(clock))
            .onConflict(USER.ID)
            .doNothing()

        val insertRowCount = query.execute()

        return if (insertRowCount != 0) user.id else null
    }

    override fun update(user: NotCompletedUser) {
        val userUpdateQuery = dslContext.update(USER)
            .set(USER.FIRSTNAME, user.firstName?.value)
            .set(USER.LASTNAME, user.lastName?.value)
            .set(USER.BIRTHDATE, user.birthDate?.value)
            .set(USER.STATUS, user.status)
            .set(USER.COMPLETED, user.completed)
            .set(USER.ROLES, user.roles?.allAsString()?.toTypedArray())
            .set(USER.MODIFIED, LocalDateTime.now(clock))
            .where(
                USER.ID.eq(user.id.value)
                    .and(USER.COMPLETED.eq(false))
                    .and(USER.ENABLED.eq(true))
            )

        userUpdateQuery.execute()
    }

    private fun UserRecord.toNotCompletedUser(): NotCompletedUser =
        NotCompletedUser(
            id = UserId(id),
            email = Email(email),
            password = null,
            firstName = firstname?.let { FirstName(it) },
            lastName = lastname?.let { LastName(it) },
            birthDate = birthdate?.let { BirthDate(it) },
            lastConnection = lastconnection,
            roles = toDomain(roles),
            status = status,
            completed = completed,
            enabled = enabled,
        )

    private fun toDomain(rawRoles: Array<String?>?): Roles? {
        val roles = rawRoles?.mapNotNull {
            when (it) {
                Roles.Role.ROLE_USER.name -> Roles.Role.ROLE_USER
                Roles.Role.ROLE_ADMIN.name -> Roles.Role.ROLE_ADMIN
                else -> error("Not eligible role $it")
            }
        }

        return if (roles != null) Roles(roles.toSet()) else null
    }
}
