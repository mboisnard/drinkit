package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.common.MessageContent
import com.drinkit.common.MessageSender
import com.drinkit.common.Recipient
import com.drinkit.common.SendMessageCommand
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.cqrs.Command
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.documentation.fcis.FunctionalCore
import com.drinkit.documentation.fcis.ImperativeShell
import com.drinkit.user.SendVerificationToken.Result.Forbidden
import com.drinkit.user.SendVerificationToken.Result.Success
import com.drinkit.user.SendVerificationToken.Result.UserNotFound
import com.drinkit.user.VerificationTokenSendingDecider.Decision.AlreadyVerified
import com.drinkit.user.VerificationTokenSendingDecider.Decision.CanSendToken
import com.drinkit.user.VerificationTokenSendingDecider.Decision.Unauthorized
import com.drinkit.user.core.Email
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserId
import com.drinkit.user.core.VerificationToken
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.VerificationTokens
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Command
data class SendVerificationTokenCommand(
    val author: Author,
    val locale: Locale,
)

@Transactional
@Service
@Usecase @ImperativeShell
class SendVerificationToken(
    private val userEvents: UserEvents,
    private val generateVerificationToken: GenerateVerificationToken,
    private val verificationTokens: VerificationTokens,
    private val messageSender: MessageSender,
) {
    sealed interface Result {
        data class Success(val token: VerificationToken): Result
        object AlreadyVerified : Result
        object UserNotFound : Result
        object Forbidden : Result
    }

    private val logger = KotlinLogging.logger {}

    fun invoke(userId: UserId, command: SendVerificationTokenCommand): Result {
        logger.debug { "Sending verification token to $userId" }

        val decision = userEvents.findAllBy(userId)
            ?.let {
                VerificationTokenSendingDecider.decide(
                    decision = UserDecision.from(it),
                    command = command,
                )
            }
            ?: return UserNotFound

        return when (decision) {
            is CanSendToken -> {
                val token = generateVerificationToken.invoke(decision.userId)
                verificationTokens.createOrUpdate(token)

                messageSender.send(
                    SendMessageCommand(
                        content = MessageContent("Verification code", token.value),
                        locale = command.locale,
                        recipient = Recipient(decision.email.value)
                    )
                )

                logger.debug { "Verification token sent to user: ${decision.userId}, email: ${decision.email}" }

                return Success(token)
            }
            AlreadyVerified -> Result.AlreadyVerified
            Unauthorized -> Forbidden
        }
    }
}

@Aggregate @FunctionalCore
internal object VerificationTokenSendingDecider {

    sealed interface Decision {
        data class CanSendToken(val userId: UserId, val email: Email) : Decision
        object AlreadyVerified : Decision
        object Unauthorized : Decision
    }

    fun decide(
        decision: UserDecision,
        command: SendVerificationTokenCommand
    ): Decision {
        if (!decision.canEdit(command.author)) {
            return Unauthorized
        }

        if (decision.isVerified) {
            return AlreadyVerified
        }

        return CanSendToken(
            userId = decision.id,
            email = decision.email,
        )
    }
}