package com.drinkit.mail.infra

import com.drinkit.mail.EmailSender
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
    fun smtpEmailSender(mailSender: JavaMailSender): EmailSender = SpringEmailSender(mailSender)

    @Bean
    @ConditionalOnMissingBean
    fun loggingEmailSender(): EmailSender = LogEmailSender()
}
