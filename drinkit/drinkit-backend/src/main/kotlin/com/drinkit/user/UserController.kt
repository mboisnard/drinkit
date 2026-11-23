package com.drinkit.user

import com.drinkit.api.generated.api.UserApi
import com.drinkit.api.generated.model.ChangeLocaleRequest
import com.drinkit.api.generated.model.ConnectedUserInformation
import com.drinkit.api.generated.model.Role
import com.drinkit.i18n.CurrentLocale
import com.drinkit.security.CurrentUser
import com.drinkit.user.core.ChooseLanguage
import com.drinkit.user.core.User
import com.drinkit.user.spi.Users
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.Locale

@RestController
internal class UserController(
    private val users: Users,
    private val changePreferences: ChangePreferences,
    private val currentUser: CurrentUser,
    private val currentLocale: CurrentLocale,
) : UserApi {

    @Transactional(readOnly = true)
    override fun getConnectedUserInfo(): ResponseEntity<ConnectedUserInformation> {
        val user = users.findEnabledBy(currentUser.get().userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        return ResponseEntity.ok(user.toInformation())
    }

    @Transactional
    override fun changeLocale(changeLocaleRequest: ChangeLocaleRequest): ResponseEntity<Unit> {
        val connectedUser = currentUser.get()
        val language = Locale.of(changeLocaleRequest.locale.value)
        val command = ChangePreferencesCommand(author = connectedUser.author, change = ChooseLanguage(language))

        val refusal = when (changePreferences.invoke(connectedUser.userId, command)) {
            ChangePreferences.Result.Success -> {
                currentLocale.keepForTheSession(language)
                return ResponseEntity.noContent().build()
            }

            ChangePreferences.Result.Forbidden -> HttpStatus.FORBIDDEN

            ChangePreferences.Result.UserNotFound -> HttpStatus.NOT_FOUND
        }

        throw ResponseStatusException(refusal)
    }

    private fun User.toInformation() = ConnectedUserInformation(
        roles = roles.values.map { Role.forValue(it.name) },
        locale = preferences.language.language,
        firstname = profile?.firstName?.value,
        lastname = profile?.lastName?.value,
        birthdate = profile?.birthDate?.value,
    )
}
