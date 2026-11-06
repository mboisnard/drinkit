package com.drinkit.user

import com.drinkit.user.core.Author
import com.drinkit.user.core.UserDecision
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId
import java.time.OffsetDateTime

fun UserId.toConnectedAuthor() = Author.Connected(this)

inline fun UserHistory.appendRemainingEvent(eventSupplier: (UserHistory) -> List<UserEvent>): UserHistory = this.copy(
    remainingEvents = this.remainingEvents + eventSupplier(this),
)

fun UserHistory.withProfileInformationCompleted(command: CompleteProfileInformationCommand) =
    this.appendRemainingEvent {
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

fun UserHistory.withDeleted(command: DeleteUserCommand, date: OffsetDateTime = OffsetDateTime.now()) =
    this.appendRemainingEvent {
        val decision = UserDeletionDecider.decide(
            decision = UserDecision.from(it),
            command = command,
            date = date,
        )

        when (decision) {
            is UserDeletionDecider.Decision.EventToPersist -> listOf(decision.event)
            else -> emptyList()
        }
    }

fun UserHistory.withReactivated(command: ReactivateUserCommand, date: OffsetDateTime = OffsetDateTime.now()) =
    this.appendRemainingEvent {
        val decision = ReactivationDecider.decide(
            decision = UserDecision.from(it),
            command = command,
            date = date,
        )

        when (decision) {
            is ReactivationDecider.Decision.EventToPersist -> listOf(decision.event)
            else -> emptyList()
        }
    }

fun UserHistory.toUserDecision() = UserDecision.from(this)
