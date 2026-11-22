package com.drinkit.user.signin

import com.drinkit.notification.core.Notification
import com.drinkit.notification.core.NotificationContent
import com.drinkit.notification.core.Recipient
import com.drinkit.notification.spi.NotificationSender
import com.drinkit.security.SignInCodeSender
import org.springframework.stereotype.Component
import java.util.Locale

/** Queues the email, so that the answer never waits for the mail server. */
@Component
internal class SignInCodeEmails(private val notificationSender: NotificationSender) : SignInCodeSender {

    override fun send(login: String, code: String, locale: Locale) = notificationSender.queue(
        Notification(
            content = NotificationContent(
                title = "Your DrinkIt sign-in code",
                content = "Your code is $code. It expires in a few minutes. " +
                    "If you did not ask to sign in to DrinkIt, ignore this email.",
            ),
            locale = locale,
            recipient = Recipient(login),
        ),
    )
}
