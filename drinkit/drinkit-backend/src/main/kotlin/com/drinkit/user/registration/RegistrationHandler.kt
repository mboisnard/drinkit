package com.drinkit.user.registration

import com.drinkit.messaging.EventHandler
import org.springframework.stereotype.Component

@Component
class RegistrationHandler(
    private val validateEmail: ValidateEmail,
) {

    @EventHandler
    fun sendVerificationTokenToTheCreatedUser(event: UserCreated) {
        validateEmail.sendVerificationTokenToUser(
            userId = event.userId,
            locale = event.locale
        )
    }
}
