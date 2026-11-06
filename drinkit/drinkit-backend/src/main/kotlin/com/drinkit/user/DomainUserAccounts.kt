package com.drinkit.user

import com.drinkit.common.CorrelationId
import com.drinkit.security.UserAccount
import com.drinkit.security.UserAccounts
import com.drinkit.user.core.Author
import com.drinkit.user.core.Email
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.Users
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Component
internal class DomainUserAccounts(
    private val users: Users,
    private val createNewUser: CreateNewUser,
    private val reactivateUser: ReactivateUser,
) : UserAccounts {

    @Transactional(readOnly = true)
    override fun findByLogin(login: String): UserAccount? = Email(login)
        .takeIf { it.validate().isEmpty() }
        ?.let(users::findEnabledBy)
        ?.toAccount()

    @Transactional(readOnly = true)
    override fun findById(id: String): UserAccount? = users.findEnabledBy(UserId(id))?.toAccount()

    @Transactional
    override fun register(login: String, locale: Locale): UserAccount? {
        val email = Email(login)
        return users.findDeletedBy(email)?.let(::reactivate) ?: create(email, locale)
    }

    // The code proved that the caller owns the email of the deleted user, who is the author of its own comeback
    private fun reactivate(user: User): UserAccount? =
        when (val result = reactivateUser.invoke(user.id, ReactivateUserCommand(Author.Connected(user.id)))) {
            is ReactivateUser.Result.Reactivated -> result.user.toAccount()

            ReactivateUser.Result.UserNotFound,
            ReactivateUser.Result.Forbidden,
            ReactivateUser.Result.NotDeleted,
            ReactivateUser.Result.RetentionOver,
            -> null
        }

    private fun create(email: Email, locale: Locale): UserAccount? {
        val command = CreateNewUserCommand(Author.Unlogged(CorrelationId.create()), email, locale)
        return when (val result = createNewUser.invoke(command)) {
            is CreateNewUser.Result.UserCreated -> result.user.toAccount()

            CreateNewUser.Result.UserAlreadyExists,
            CreateNewUser.Result.InvalidEmail,
            -> null
        }
    }

    private fun User.toAccount() = UserAccount(id = id.value, roles = roles.allAsString())
}
