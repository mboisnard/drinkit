package com.drinkit.user.signin

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.PlatformEventPublisher
import com.drinkit.notification.core.Notification
import com.drinkit.notification.core.NotificationContent
import com.drinkit.notification.core.Recipient
import com.drinkit.notification.spi.NotificationSender
import com.drinkit.security.SignInCodeSender
import org.springframework.stereotype.Component
import java.util.Locale

internal data class SignInCodeRequested(val email: String, val code: String, val locale: Locale) :
    PlatformEvent<SignInCodeRequested> {

    // The default one prints the email and the code wherever the event is logged
    override fun toString() = "SignInCodeRequested"
}

/** Publishes the code, and emails it from the handler, so that the answer never waits for the mail server. */
@Component
internal class SignInCodeEmails(
    private val publisher: PlatformEventPublisher,
    private val notificationSender: NotificationSender,
) : SignInCodeSender {

    override fun send(login: String, code: String, locale: Locale) {
        publisher.publish(SignInCodeRequested(login, code, locale))
    }

    @PlatformEventHandler("send.sign.in.code.queue")
    fun sendTheCode(event: SignInCodeRequested) {
        notificationSender.queue(
            Notification(
                content = NotificationContent(
                    title = "Your DrinkIt sign-in code",
                    content = "Your code is ${event.code}. It expires in a few minutes. " +
                        "If you did not ask to sign in to DrinkIt, ignore this email.",
                ),
                locale = event.locale,
                recipient = Recipient(event.email),
            ),
        )
    }
}
