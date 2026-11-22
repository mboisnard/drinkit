package com.drinkit.user.signin

import com.drinkit.i18n.CurrentLocale
import com.drinkit.notification.core.Notification
import com.drinkit.notification.core.NotificationContent
import com.drinkit.notification.core.Recipient
import com.drinkit.notification.spi.NotificationSender
import com.drinkit.security.SignInCodeSender
import org.springframework.stereotype.Component

/** Queues the email, so that the answer never waits for the mail server. */
@Component
internal class SignInCodeEmails(
    private val notificationSender: NotificationSender,
    private val currentLocale: CurrentLocale,
) : SignInCodeSender {

    override fun send(login: String, code: String) = notificationSender.queue(
        Notification(
            content = NotificationContent("mail.signInCode.subject", "mail.signInCode.body", mapOf("code" to code)),
            locale = currentLocale.get(),
            recipient = Recipient(login),
        ),
    )
}
