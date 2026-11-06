package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

/** The user of the current request, and the way to sign them in or reload their roles. */
@TechStarterTool
interface CurrentUser {

    /** The connected user. An anonymous caller gets a 401. */
    fun get(): ConnectedUser

    /** The connected user, or null for an anonymous caller or outside a request. */
    fun orNull(): ConnectedUser?

    /** Signs this request in as [userId], with a new session id, as a sign-in would, for a user proven another way. */
    fun signIn(userId: String): ConnectedUser

    /** Reloads the connected user's roles from [UserAccounts] under a new session id. Null when the account is gone. */
    fun refresh(): ConnectedUser?
}

/** The user of the current request: the [id] the application gave it, and its [roles], such as `ROLE_USER`. */
@TechStarterTool
data class ConnectedUser(val id: String, val roles: Set<String>)
