package com.drinkit.user.registration

import com.drinkit.common.MessageContent
import com.drinkit.common.MessageSender
import com.drinkit.common.Recipient
import com.drinkit.common.SendMessageCommand
import com.drinkit.user.GenerateVerificationToken
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.VerificationTokens
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Locale

@Service
class ValidateEmail(
    private val notCompletedUsers: NotCompletedUsers,
    private val generateVerificationToken: GenerateVerificationToken,
    private val verificationTokens: VerificationTokens,
    private val messageSender: MessageSender,
    private val clock: Clock,
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun sendVerificationTokenToUser(userId: UserId, locale: Locale) {
        logger.debug { "Sending verification token to $userId" }

        val notCompletedUser = notCompletedUsers.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        val token = generateVerificationToken.invoke(notCompletedUser.id)

        verificationTokens.createOrUpdate(token)

        messageSender.send(
            SendMessageCommand(
                content = MessageContent(
                    title = "Verification code",
                    content = token.value,
                ),
                locale = locale,
                recipient = Recipient(notCompletedUser.email.value)
            )
        )

        logger.info { "Verification token sent to user: ${notCompletedUser.id}, email: ${notCompletedUser.email}" }
    }

    @Transactional
    fun validateVerificationToken(userId: UserId, token: String) {
        val notCompletedUser = notCompletedUsers.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        val verificationToken = verificationTokens.findBy(notCompletedUser.id, token)
            ?: throw IllegalArgumentException("Token not found")

        val tokenIsValid = verificationToken.expiryDate.isAfter(OffsetDateTime.now(clock))

        if (tokenIsValid) {
            notCompletedUsers.update(
                notCompletedUser.withEmailVerified()
            )
        }

        verificationTokens.deleteBy(notCompletedUser.id)
    }
}
