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
import org.springframework.context.annotation.PropertySource
import org.springframework.mail.javamail.JavaMailSender

@AutoConfiguration(after = [MailSenderAutoConfiguration::class])
@ConditionalOnProperty(prefix = "starters.mail", name = ["enabled"], matchIfMissing = true)
@PropertySource("classpath:META-INF/drinkit/mail-defaults.properties")
internal class MailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JavaMailSender::class)
    fun smtpEmailSender(mailSender: JavaMailSender, publisher: ObjectProvider<PlatformEventPublisher>): EmailSender {
        val available = checkNotNull(publisher.ifAvailable) {
            "The mail starter delivers through the messaging starter: keep starters.messaging.enabled"
        }
        return SpringEmailSender(available, mailSender)
    }

    @Bean
    @ConditionalOnMissingBean
    fun noopEmailSender(): EmailSender = NoopEmailSender()
}
