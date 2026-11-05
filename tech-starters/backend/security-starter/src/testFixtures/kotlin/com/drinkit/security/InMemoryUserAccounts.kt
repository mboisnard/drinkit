package com.drinkit.security

import java.util.concurrent.ConcurrentHashMap

/** Each account with the login it is found by. */
class InMemoryUserAccounts(vararg accounts: Pair<String, UserAccount>) : UserAccounts {

    private val accounts = ConcurrentHashMap(accounts.associateBy { (_, account) -> account.id })

    fun save(login: String, account: UserAccount) {
        accounts[account.id] = login to account
    }

    fun remove(id: String) {
        accounts.remove(id)
    }

    override fun findByLogin(login: String): UserAccount? = accounts.values.firstOrNull { it.first == login }?.second

    override fun findById(id: String): UserAccount? = accounts[id]?.second
}
