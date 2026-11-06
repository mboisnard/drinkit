package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface CurrentUser {

    fun get(): ConnectedUser

    fun orNull(): ConnectedUser?

    fun signIn(userId: String): ConnectedUser

    /** Null when the account is gone, and the caller is then signed out. */
    fun refresh(): ConnectedUser?
}

@TechStarterTool
data class ConnectedUser(val id: String, val roles: Set<String>)
