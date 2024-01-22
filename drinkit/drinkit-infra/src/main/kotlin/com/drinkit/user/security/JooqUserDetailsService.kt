package com.drinkit.user.security

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
internal class JooqUserDetailsService(
    private val securityUserRepository: SecurityUserRepository,
): UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        return securityUserRepository.findByEmail(username)
            ?: throw UsernameNotFoundException(username)
    }
}