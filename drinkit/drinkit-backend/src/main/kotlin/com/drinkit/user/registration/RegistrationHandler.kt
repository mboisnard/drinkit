package com.drinkit.user.registration

import com.drinkit.messaging.PlatformEventHandler
import org.springframework.stereotype.Component

@Component
class RegistrationHandler(
    private val validateEmail: ValidateEmail,
) {

    @PlatformEventHandler
    fun sendVerificationTokenToTheCreatedUser(event: UserCreated) {
        validateEmail.sendVerificationTokenToUser(
            userId = event.userId,
            locale = event.locale
        )
    }
}
