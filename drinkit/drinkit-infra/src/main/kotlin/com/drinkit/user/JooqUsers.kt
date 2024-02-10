package com.drinkit.user

import com.drinkit.generated.jooq.tables.Role.Companion.ROLE
import com.drinkit.generated.jooq.tables.User.Companion.USER
import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import org.jooq.DSLContext
import org.jooq.impl.DSL.multiset
import org.springframework.stereotype.Repository

@Repository
internal class JooqUsers(
    private val dslContext: DSLContext,
) : Users {

    override fun findById(userId: UserId): User? {
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
                    .and(USER.COMPLETED.eq(true))
                    .and(USER.ENABLED.eq(true))
            )

        return query
            .fetchSequence(userWithRolesRecordMapper)
            .firstOrNull()
            ?.toUser()
    }

    private fun JooqUserWithRolesView.toUser(): User =
        User(
            id = UserId(user.id),
            firstname = FirstName(user.firstname!!),
            lastName = LastName(user.lastname!!),
            birthDate = BirthDate(user.birthdate!!),
            email = Email(user.email),
            lastConnection = user.lastconnection,
            roles = Roles(roles.map {
                when (it.authority) {
                    Roles.Role.ROLE_USER.name -> Roles.Role.ROLE_USER
                    Roles.Role.ROLE_ADMIN.name -> Roles.Role.ROLE_ADMIN
                    else -> throw IllegalStateException("Not eligible role $it")
                }
            }.toSet()),
            enabled = user.enabled,
        )
}