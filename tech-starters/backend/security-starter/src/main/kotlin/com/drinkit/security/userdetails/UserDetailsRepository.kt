package com.drinkit.security.userdetails

import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import com.drinkit.security.generated.jooq.tables.User.Companion.USER
import com.drinkit.security.generated.jooq.tables.records.UserRecord
import org.jooq.DSLContext
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Repository

internal fun interface UserDetailsRepository {
    fun findByEmail(email: String): InternalUserDetails?
}

@Repository
internal class JooqUserDetailsRepository(
    private val dslContext: DSLContext,
) : UserDetailsRepository {

    override fun findByEmail(email: String): InternalUserDetails? {
        val query = dslContext.select(
            allFields(USER),
        )
            .from(USER)
            .where(USER.EMAIL.eq(email))

        return query
            .fetchSequence({ it.value1()})
            .firstOrNull()
            ?.toSecurityUser()
    }

    private fun UserRecord.toSecurityUser(): InternalUserDetails =
        InternalUserDetails(
            id = id,
            username = email,
            password = password,
            authorities = roles?.mapNotNull { it?.let { SimpleGrantedAuthority(it) } }?.toSet() ?: emptySet(),
            enabled = enabled,
        )
}
