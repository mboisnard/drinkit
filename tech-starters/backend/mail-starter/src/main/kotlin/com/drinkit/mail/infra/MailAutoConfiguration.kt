package com.drinkit.mail.infra

import com.drinkit.mail.EmailSender
import com.drinkit.messaging.PlatformEventPublisher
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender

@AutoConfiguration(after = [MailSenderAutoConfiguration::class])
@ConditionalOnProperty(prefix = "starters.mail", name = ["enabled"], matchIfMissing = true)
internal class MailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmailSender::class)
    @ConditionalOnBean(JavaMailSender::class)
    fun queuedEmailSender(publisher: ObjectProvider<PlatformEventPublisher>): QueuedEmailSender {
        val available = checkNotNull(publisher.ifAvailable) {
            "The mail starter delivers through the messaging starter: keep starters.messaging.enabled"
        }

        return QueuedEmailSender(available)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(QueuedEmailSender::class)
    fun smtpEmailDelivery(mailSender: JavaMailSender): SmtpEmailDelivery = SmtpEmailDelivery(mailSender)

    @Bean
    @ConditionalOnMissingBean(EmailSender::class)
    fun noopEmailSender(): NoopEmailSender = NoopEmailSender()
}
