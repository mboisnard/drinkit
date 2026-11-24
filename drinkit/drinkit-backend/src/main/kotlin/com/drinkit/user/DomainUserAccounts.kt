package com.drinkit.user

import com.drinkit.common.VisitorId
import com.drinkit.i18n.CurrentLocale
import com.drinkit.security.CurrentVisitor
import com.drinkit.security.UserAccount
import com.drinkit.security.UserAccounts
import com.drinkit.user.core.Author
import com.drinkit.user.core.Email
import com.drinkit.user.core.Preferences
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.Users
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class DomainUserAccounts(
    private val users: Users,
    private val createNewUser: CreateNewUser,
    private val reactivateUser: ReactivateUser,
    private val currentVisitor: CurrentVisitor,
    private val currentLocale: CurrentLocale,
) : UserAccounts {

    @Transactional(readOnly = true)
    override fun findByLogin(login: String): UserAccount? = Email(login)
        .takeIf { it.validate().isEmpty() }
        ?.let(users::findEnabledBy)
        ?.toAccount()

    @Transactional(readOnly = true)
    override fun findById(id: String): UserAccount? = UserId.parseOrNull(id)?.let(users::findEnabledBy)?.toAccount()

    @Transactional(readOnly = true)
    override fun signedIn(id: String) {
        val user = UserId.parseOrNull(id)?.let(users::findEnabledBy) ?: return

        currentLocale.keepForTheSession(user.preferences.language)
    }

    @Transactional
    override fun register(login: String): UserAccount? {
        val email = Email(login)
        return users.findDeletedBy(email)?.let(::reactivate) ?: create(email)
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

    private fun create(email: Email): UserAccount? {
        val command = CreateNewUserCommand(
            author = Author.Unlogged(VisitorId(currentVisitor.id())),
            email = email,
            preferences = Preferences(language = currentLocale.get()),
        )
        return when (val result = createNewUser.invoke(command)) {
            is CreateNewUser.Result.UserCreated -> result.user.toAccount()

            CreateNewUser.Result.UserAlreadyExists,
            CreateNewUser.Result.InvalidEmail,
            -> null
        }
    }

    private fun User.toAccount() = UserAccount(id = id.value, roles = roles.allAsString())
}
