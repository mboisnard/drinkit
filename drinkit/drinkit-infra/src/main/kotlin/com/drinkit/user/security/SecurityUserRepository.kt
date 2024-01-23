package com.drinkit.user.security

import com.drinkit.common.JooqUtils.allFields
import com.drinkit.common.JooqUtils.fetchSequence
import com.drinkit.generated.jooq.tables.records.RoleRecord
import com.drinkit.generated.jooq.tables.records.UserRecord
import com.drinkit.generated.jooq.tables.references.ROLE
import com.drinkit.generated.jooq.tables.references.USER
import com.drinkit.user.UserId
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

    fun toSecurityUser(): SecurityUser =
        SecurityUser(
            id = user.id,
            username = user.email,
            password = user.password,
            authorities = roles.map { role -> SimpleGrantedAuthority(role.authority) }.toSet(),
            enabled = user.enabled,
        )
}

fun interface SecurityUserRepository {
    fun findByEmail(email: String): SecurityUser?
}

@Repository
internal class JooqSecurityUserRepository(
    private val dslContext: DSLContext,
) : SecurityUserRepository {

    private val mapper = RecordMapper<Record2<UserRecord, List<RoleRecord>>, JooqUserWithRolesView> {
        JooqUserWithRolesView(
            user = it.value1(),
            roles = it.value2()
        )
    }

    override fun findByEmail(email: String): SecurityUser? {
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