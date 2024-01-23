package com.drinkit.user

import com.drinkit.generated.jooq.tables.Role.Companion.ROLE
import com.drinkit.generated.jooq.tables.User.Companion.USER
import com.drinkit.generated.jooq.tables.records.RoleRecord
import com.drinkit.generated.jooq.tables.records.UserRecord
import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.RecordMapper
import org.jooq.impl.DSL.multiset
import org.springframework.stereotype.Repository

internal class JooqUserWithRolesView(
    val user: UserRecord,
    val roles: List<RoleRecord>,
) : Record by user {

    fun toUser(): User =
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

@Repository
internal class JooqUserRepository(
    private val dslContext: DSLContext,
) : UserRepository {

    private val mapper = RecordMapper<Record2<UserRecord, List<RoleRecord>>, JooqUserWithRolesView> {
        JooqUserWithRolesView(
            user = it.value1(),
            roles = it.value2()
        )
    }

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
            .where(USER.ID.eq(userId.value))

        return query
            .fetchSequence(mapper)
            .firstOrNull()
            ?.toUser()
    }
}