package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.Locale

/** A disabled or deleted account is not found. */
@TechStarterTool
interface UserAccounts {

    fun findByLogin(login: String): UserAccount?

    fun findById(id: String): UserAccount?

    /**
     * The account of [login], which a sign-in code has just proved to belong to the caller, when [findByLogin] found
     * none: a new account, or one the application brings back. Null refuses the sign-in.
     */
    fun register(login: String, locale: Locale): UserAccount?
}

/** [roles] are full authority names, such as `ROLE_USER`. */
@TechStarterTool
data class UserAccount(val id: String, val roles: Set<String>)
