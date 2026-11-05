package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

/**
 * The port the application implements to give its users to the starter, which reads no table of the application.
 * A disabled or deleted account is not found.
 */
@TechStarterTool
interface UserAccounts {

    fun findByLogin(login: String): UserAccount?

    fun findById(id: String): UserAccount?
}

/** An account as the starter authenticates it. [roles] are full authority names, such as `ROLE_USER`. */
@TechStarterTool
data class UserAccount(val id: String, val passwordHash: String, val roles: Set<String>)
