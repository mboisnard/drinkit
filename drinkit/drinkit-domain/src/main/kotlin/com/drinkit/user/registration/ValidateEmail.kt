package com.drinkit.user.registration

import com.drinkit.common.MessageContent
import com.drinkit.common.MessageSender
import com.drinkit.common.Recipient
import com.drinkit.common.SendMessageCommand
import com.drinkit.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
@Transactional
class ValidateEmail(
    private val userRegistrationRepository: UserRegistrationRepository,
    private val generateVerificationToken: GenerateVerificationToken,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val messageSender: MessageSender,
): RegistrationStep {

    private val logger = KotlinLogging.logger { }

    fun sendVerificationTokenToUser(userId: UserId, locale: Locale) {
        logger.debug { "Sending verification token to $userId" }

        val notCompletedUser = userRegistrationRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        val token = generateVerificationToken(notCompletedUser.id)

        verificationTokenRepository.createOrUpdate(token)

        messageSender.send(SendMessageCommand(
            content = MessageContent(
                title = "Verification code",
                content = token.token,
            ),
            locale = locale,
            recipient = Recipient(notCompletedUser.email.value)
        ))

        logger.info { "Verification token sent to user: ${notCompletedUser.id}, email: ${notCompletedUser.email}" }
    }

    override fun status(): String = "EMAIL_VERIFIED"
}