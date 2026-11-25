package com.drinkit.security

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserAccounts(
    vararg accounts: UserAccount,
    private val newAccountRoles: Set<String> = setOf("ROLE_REGISTRATION_IN_PROGRESS"),
) : UserAccounts {

    private val accounts = ConcurrentHashMap(accounts.associateBy { it.id })
    private val refusedLogins = ConcurrentHashMap.newKeySet<String>()
    private val signIns = ConcurrentHashMap<String, Int>()

    fun save(account: UserAccount) {
        accounts[account.id] = account
    }

    fun remove(id: String) {
        accounts.remove(id)
    }

    fun signInsOf(id: String): Int = signIns[id] ?: 0

    fun refuseRegistrationOf(login: String) {
        refusedLogins += login
    }

    override fun findByLogin(login: String): UserAccount? = accounts.values.firstOrNull { it.login == login }

    override fun findById(id: String): UserAccount? = accounts[id]

    override fun register(login: String): UserAccount? = login
        .takeUnless { it in refusedLogins }
        ?.let { UserAccount(UUID.randomUUID().toString(), login, newAccountRoles).also(::save) }

    override fun signedIn(id: String) {
        signIns.merge(id, 1, Int::plus)
    }
}
