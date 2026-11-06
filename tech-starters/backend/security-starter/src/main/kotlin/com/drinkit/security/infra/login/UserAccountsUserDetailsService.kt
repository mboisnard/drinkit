package com.drinkit.security.infra.login

import com.drinkit.security.UserAccount
import com.drinkit.security.UserAccounts
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/** Names the user by its id, so that neither the session nor the logs hold the login, often an email. */
internal class UserAccountsUserDetailsService(private val accounts: UserAccounts) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails =
        accounts.findByLogin(username)?.toUserDetails() ?: throw UsernameNotFoundException("Unknown login")

    fun loadUserById(id: String): UserDetails =
        accounts.findById(id)?.toUserDetails() ?: throw UsernameNotFoundException("Unknown account")
}

internal fun UserAccount.toUserDetails(): UserDetails =
    User.withUsername(id).password("").authorities(roles.map(::SimpleGrantedAuthority)).build()
