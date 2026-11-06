package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.Locale

/**
 * The port the application implements to give its users to the starter, which reads no table of the application.
 * A disabled or deleted account is not found.
 */
@TechStarterTool
interface UserAccounts {

    /** The account of [login], such as an email, to sign in. */
    fun findByLogin(login: String): UserAccount?

    /** The account of [id], to restore a remembered sign-in or reload the roles of a session. */
    fun findById(id: String): UserAccount?

    /**
     * The account of [login], which a sign-in code has just proved to belong to the caller, when [findByLogin] found
     * none: a new account, or one the application brings back. Null refuses the sign-in.
     */
    fun register(login: String, locale: Locale): UserAccount?
}

/** An account as the starter authenticates it. [roles] are full authority names, such as `ROLE_USER`. */
@TechStarterTool
data class UserAccount(val id: String, val roles: Set<String>)
