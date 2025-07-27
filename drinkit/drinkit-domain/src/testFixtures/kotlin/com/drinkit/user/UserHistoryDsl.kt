package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId
import com.drinkit.user.core.VerificationToken
import java.time.OffsetDateTime

fun UserId.toConnectedAuthor() = Author.Connected(this)

inline fun UserHistory.appendRemainingEvent(
    eventSupplier: (UserHistory) -> List<UserEvent>,
): UserHistory = this.copy(
    remainingEvents = this.remainingEvents + eventSupplier(this),
)

fun UserHistory.withProfileInformationCompleted(command: CompleteProfileInformationCommand) = this.appendRemainingEvent {
    val decision = ProfileCompletionDecider.invoke(
        decision = UserDecision.from(it),
        command = command,
        date = OffsetDateTime.now(),
    )

    when (decision) {
        is ProfileCompletionDecider.Decision.EventToPersist -> listOf(decision.event)
        else -> emptyList()
    }
}

fun UserHistory.withPromotedAsAdmin(command: PromoteAsAdminCommand) = this.appendRemainingEvent {
    val decision = AdminPromotionDecider.decide(
        decision = UserDecision.from(it),
        command = command,
        date = OffsetDateTime.now(),
    )

    when (decision) {
        is AdminPromotionDecider.Decision.EventToPersist -> listOf(decision.event)
        else -> emptyList()
    }
}

fun UserHistory.withDeleted(command: DeleteUserCommand) = this.appendRemainingEvent {
    val decision = UserDeletionDecider.decide(
        decision = UserDecision.from(it),
        command = command,
        date = OffsetDateTime.now(),
    )

    when (decision) {
        is UserDeletionDecider.Decision.EventToPersist -> listOf(decision.event)
        else -> emptyList()
    }
}

fun UserHistory.withVerified(command: ConfirmVerificationTokenCommand) = this.appendRemainingEvent {
    val userDecision = UserDecision.from(it)
    val decision = VerificationTokenDecider.decide(
        decision = userDecision,
        command = command,
        foundToken = VerificationToken(
            userId = userDecision.id,
            value = command.token,
            expiryDate = OffsetDateTime.now().plusHours(1)
        ),
        date = OffsetDateTime.now()
    )

    when (decision) {
        is VerificationTokenDecider.Decision.EventToPersist -> listOf(decision.event)
        else -> emptyList()
    }
}

fun UserHistory.toUserDecision() = UserDecision.from(this)