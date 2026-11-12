package com.drinkit.security

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

class InMemoryCurrentUser(private val accounts: UserAccounts = InMemoryUserAccounts()) : CurrentUser {

    private var user: ConnectedUser? = null

    override fun get(): ConnectedUser = orNull() ?: throw AuthenticationCredentialsNotFoundException("Anonymous")

    override fun orNull(): ConnectedUser? = user

    override fun signIn(userId: String): ConnectedUser {
        val account = requireNotNull(accounts.findById(userId)) { "No account $userId to sign in" }

        return ConnectedUser(account.id, account.roles).also { user = it }
    }

    fun signOut() {
        user = null
    }

    override fun refresh(): ConnectedUser? {
        val current = user ?: return null

        user = accounts.findById(current.id)?.let { ConnectedUser(it.id, it.roles) }

        return user
    }
}
