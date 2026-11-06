package com.drinkit.security

import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserAccounts(
    vararg accounts: Pair<String, UserAccount>,
    private val newAccountRoles: Set<String> = setOf("ROLE_REGISTRATION_IN_PROGRESS"),
) : UserAccounts {

    private val accounts = ConcurrentHashMap(accounts.associateBy { (_, account) -> account.id })
    private val refusedLogins = ConcurrentHashMap.newKeySet<String>()

    fun save(login: String, account: UserAccount) {
        accounts[account.id] = login to account
    }

    fun remove(id: String) {
        accounts.remove(id)
    }

    fun refuseRegistrationOf(login: String) {
        refusedLogins += login
    }

    override fun findByLogin(login: String): UserAccount? = accounts.values.firstOrNull { it.first == login }?.second

    override fun findById(id: String): UserAccount? = accounts[id]?.second

    override fun register(login: String, locale: Locale): UserAccount? = login
        .takeUnless { it in refusedLogins }
        ?.let { UserAccount(UUID.randomUUID().toString(), newAccountRoles).also { account -> save(login, account) } }
}
