package com.drinkit.user

import com.drinkit.security.UserAccount
import com.drinkit.security.UserAccounts
import com.drinkit.user.core.Email
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.Users
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class DomainUserAccounts(private val users: Users) : UserAccounts {

    @Transactional(readOnly = true)
    override fun findByLogin(login: String): UserAccount? = Email(login)
        .takeIf { it.validate().isEmpty() }
        ?.let(users::findEnabledBy)
        ?.toAccount()

    @Transactional(readOnly = true)
    override fun findById(id: String): UserAccount? = users.findEnabledBy(UserId(id))?.toAccount()

    private fun User.toAccount() = UserAccount(
        id = id.value,
        passwordHash = password.value,
        roles = roles.allAsString(),
    )
}
