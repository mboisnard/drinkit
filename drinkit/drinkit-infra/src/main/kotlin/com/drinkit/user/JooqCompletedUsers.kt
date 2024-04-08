package com.drinkit.user

import com.drinkit.generated.jooq.tables.User.Companion.USER
import com.drinkit.generated.jooq.tables.records.UserRecord
import com.drinkit.jooq.allFields
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
internal class JooqCompletedUsers(
    private val dslContext: DSLContext,
) : CompletedUsers {

    override fun findById(userId: UserId): CompletedUser? {
        val query = dslContext.select(allFields(USER))
            .from(USER)
            .where(
                USER.ID.eq(userId.value)
                    .and(USER.COMPLETED.eq(true))
                    .and(USER.ENABLED.eq(true))
            )

        return query.fetchOne { it.value1() }?.toUser()
    }

    private fun UserRecord.toUser(): CompletedUser =
        CompletedUser(
            id = UserId(id),
            firstname = FirstName(firstname!!),
            lastName = LastName(lastname!!),
            birthDate = BirthDate(birthdate!!),
            email = Email(email),
            lastConnection = lastconnection,
            roles = Roles(
                roles?.map {
                    when (it) {
                        Roles.Role.ROLE_USER.name -> Roles.Role.ROLE_USER
                        Roles.Role.ROLE_ADMIN.name -> Roles.Role.ROLE_ADMIN
                        else -> error("Not eligible role $it")
                    }
                }?.toSet() ?: emptySet()
            ),
            enabled = enabled,
        )
}
