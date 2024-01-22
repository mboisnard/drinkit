package com.drinkit.user.security

import com.drinkit.user.UserId
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

val ROLE_ADMIN = SimpleGrantedAuthority("ROLE_ADMIN")
val ROLE_USER = SimpleGrantedAuthority("ROLE_USER")

data class SecurityUser(
    val id: UserId,
    @get:JvmName("_username")
    val username: String,
    @get:JvmName("_password")
    val password: String,
    @get:JvmName("_authorities")
    val authorities: Set<GrantedAuthority>,
    @get:JvmName("_enabled")
    val enabled: Boolean,
): UserDetails {
    override fun getUsername(): String = username

    override fun getPassword(): String = password

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled
}
