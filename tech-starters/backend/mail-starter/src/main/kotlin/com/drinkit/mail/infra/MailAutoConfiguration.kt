package com.drinkit.mail.infra

import com.drinkit.mail.EmailSender
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.mail", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(MailProperties::class)
internal class MailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "starters.mail", name = ["sender"], havingValue = "smtp", matchIfMissing = true)
    fun smtpEmailSender(mailSender: ObjectProvider<JavaMailSender>, properties: MailProperties): EmailSender =
        SpringEmailSender(mailSender.ifAvailable, properties.from)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "starters.mail", name = ["sender"], havingValue = "logging")
    fun loggingEmailSender(): EmailSender = LogEmailSender()
}
