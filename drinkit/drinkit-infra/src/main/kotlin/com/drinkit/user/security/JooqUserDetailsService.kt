package com.drinkit.user.security

import com.drinkit.generated.jooq.tables.references.USER
import com.drinkit.user.UserId
import org.jooq.DSLContext
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
internal class JooqUserDetailsService(
    private val dslContext: DSLContext,
): UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        return dslContext
            .fetchOne(USER, USER.EMAIL.eq(username))
            ?.let {
                SecurityUser(
                    id = UserId(it.id!!),
                    username = it.email!!,
                    password = it.password!!,
                    authorities = emptySet(),
                    enabled = it.enabled!!,
                )
            } ?: throw UsernameNotFoundException(username)
    }
}