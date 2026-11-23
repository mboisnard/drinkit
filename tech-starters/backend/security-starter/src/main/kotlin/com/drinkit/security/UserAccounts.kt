package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

/** A disabled or deleted account is not found. */
@TechStarterTool
interface UserAccounts {

    fun findByLogin(login: String): UserAccount?

    fun findById(id: String): UserAccount?

    /**
     * The account of [login], which a sign-in code has just proved to belong to the caller, when [findByLogin] found
     * none: a new account, or one the application brings back. Null refuses the sign-in.
     */
    fun register(login: String): UserAccount?

    /**
     * Called directly in the request of each sign-in, by code, remembered or [CurrentUser.signIn], such as to keep
     * the account's language for the session.
     */
    fun signedIn(id: String) = Unit
}

/** [roles] are full authority names, such as `ROLE_USER`. */
@TechStarterTool
data class UserAccount(val id: String, val roles: Set<String>)
