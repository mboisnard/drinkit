package com.drinkit.security.userdetails


import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import com.drinkit.security.generated.jooq.tables.Role.Companion.ROLE
import com.drinkit.security.generated.jooq.tables.User.Companion.USER
import com.drinkit.security.generated.jooq.tables.records.RoleRecord
import com.drinkit.security.generated.jooq.tables.records.UserRecord

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.RecordMapper
import org.jooq.impl.DSL.multiset
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Repository

internal class JooqUserWithRolesView(
    val user: UserRecord,
    val roles: List<RoleRecord>,
) : Record by user {

    fun toSecurityUser(): InternalUserDetails =
        InternalUserDetails(
            id = user.id,
            username = user.email,
            password = user.password,
            authorities = roles.map { role -> SimpleGrantedAuthority(role.authority) }.toSet(),
            enabled = user.enabled,
        )
}

internal fun interface UserDetailsRepository {
    fun findByEmail(email: String): InternalUserDetails?
}

@Repository
internal class JooqUserDetailsRepository(
    private val dslContext: DSLContext,
) : UserDetailsRepository {

    private val mapper = RecordMapper<Record2<UserRecord, List<RoleRecord>>, JooqUserWithRolesView> {
        JooqUserWithRolesView(
            user = it.value1(),
            roles = it.value2()
        )
    }

    override fun findByEmail(email: String): InternalUserDetails? {
        val query = dslContext.select(
            allFields(USER),
            multiset(
                dslContext.select(allFields(ROLE))
                    .from(ROLE)
                    .where(ROLE.USER_ID.eq(USER.ID))
            ).convertFrom { result -> result.map { it.value1() } }.`as`("roles")
        )
            .from(USER)
            .where(USER.EMAIL.eq(email))

        return query
                .fetchSequence(mapper)
                .firstOrNull()
                ?.toSecurityUser()
    }
}