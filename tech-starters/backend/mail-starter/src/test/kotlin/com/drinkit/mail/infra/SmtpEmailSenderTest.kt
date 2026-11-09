package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.mail.EmailSender
import com.drinkit.mail.EmailSenderTestContract
import com.drinkit.mail.Mailpit
import com.drinkit.mail.MailpitIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.mail.javamail.JavaMailSender
import java.util.function.Supplier

@MailpitIntegrationTest
internal class SmtpEmailSenderTest : EmailSenderTestContract() {

    private lateinit var mailpit: Mailpit
    private lateinit var context: AnnotationConfigApplicationContext

    @BeforeEach
    fun startApplication(mailpit: Mailpit) {
        this.mailpit = mailpit
        context = AnnotationConfigApplicationContext().apply {
            register(MailApplication::class.java)
            registerBean(JavaMailSender::class.java, Supplier { mailpit.javaMailSender() })
            refresh()
        }
    }

    @AfterEach
    fun stopApplication() = context.close()

    override fun sender(): EmailSender = context.getBean<EmailSender>()

    override fun deliveredCount(email: Email): Int = mailpit.countEmailsWithSubject(email.subject.value)
}
