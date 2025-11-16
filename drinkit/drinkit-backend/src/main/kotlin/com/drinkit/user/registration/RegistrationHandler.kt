package com.drinkit.user.registration

import com.drinkit.common.Author
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.user.SendVerificationToken
import com.drinkit.user.SendVerificationTokenCommand
import com.drinkit.user.spi.UserCreated
import org.springframework.stereotype.Component

@Component
internal class RegistrationHandler(
    private val sendVerificationToken: SendVerificationToken,
) {

    @PlatformEventHandler("send.verification.token.to.created.user.queue")
    fun sendVerificationTokenToTheCreatedUser(event: UserCreated) {
        sendVerificationToken.invoke(
            userId = event.userId,
            command = SendVerificationTokenCommand(
                author = Author.Connected(event.userId), // TODO
                locale = event.locale,
            )
        )
    }
}
