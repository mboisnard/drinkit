package com.drinkit.user

import com.drinkit.generated.jooq.tables.Role.Companion.ROLE
import com.drinkit.generated.jooq.tables.User.Companion.USER
import com.drinkit.generated.jooq.tables.records.RoleRecord
import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import com.drinkit.user.registration.NotCompletedUser
import com.drinkit.user.registration.UserRegistrationRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL.multiset
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime

@Repository
internal class JooqUserRegistrationRepository(
    private val dslContext: DSLContext,
    private val clock: Clock,
) : UserRegistrationRepository {

    override fun emailExists(email: Email): Boolean {
        val count = dslContext.fetchCount(USER, USER.EMAIL.eq(email.value))
        return count != 0
    }

    override fun findById(userId: UserId): NotCompletedUser? {
        val query = dslContext.select(
            allFields(USER),
            multiset(
                dslContext.select(allFields(ROLE))
                    .from(ROLE)
                    .where(ROLE.USER_ID.eq(USER.ID))
            ).convertFrom { result -> result.map { it.value1() } }.`as`("roles")
        )
            .from(USER)
            .where(
                USER.ID.eq(userId.value)
                    .and(USER.COMPLETED.eq(false))
                    .and(USER.ENABLED.eq(true))
            )

        return query
            .fetchSequence(userWithRolesRecordMapper)
            .firstOrNull()
            ?.toNotCompletedUser()
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
        TODO("Not yet implemented")
    }

    private fun JooqUserWithRolesView.toNotCompletedUser(): NotCompletedUser =
        NotCompletedUser(
            id = UserId(user.id),
            email = Email(user.email),
            password = null,
            firstname = user.firstname?.let { FirstName(it) },
            lastName = user.lastname?.let { LastName(it) },
            birthDate = user.birthdate?.let { BirthDate(it) },
            lastConnection = user.lastconnection,
            roles = roles.toDomain(),
            status = user.status,
            completed = user.completed,
            enabled = user.enabled,
        )

    private fun List<RoleRecord>.toDomain(): Roles? {
        if (this.isEmpty())
            return null

        return Roles(this.map {
            when (it.authority) {
                Roles.Role.ROLE_USER.name -> Roles.Role.ROLE_USER
                Roles.Role.ROLE_ADMIN.name -> Roles.Role.ROLE_ADMIN
                else -> throw IllegalStateException("Not eligible role $it")
            }
        }.toSet())
    }
}