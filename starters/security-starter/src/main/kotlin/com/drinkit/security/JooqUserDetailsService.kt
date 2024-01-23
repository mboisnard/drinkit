package com.drinkit.security

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class JooqUserDetailsService(
    private val securityUserRepository: SecurityUserRepository,
): UserDetailsService {

    @Transactional(readOnly = true)
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        return securityUserRepository.findByEmail(username)
            ?: throw UsernameNotFoundException(username)
    }
}