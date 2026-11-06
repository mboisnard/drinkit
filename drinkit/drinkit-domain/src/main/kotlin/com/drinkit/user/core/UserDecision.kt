package com.drinkit.user.core

import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.event.sourcing.EventsReducer
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.user.core.Roles.Role.ROLE_ADMIN
import com.drinkit.user.core.Roles.Role.ROLE_USER
import java.time.OffsetDateTime

@Aggregate
data class UserDecision(
    val id: UserId,
    val email: Email,
    private var _sequenceId: SequenceId,
    private var _roles: Roles,
    private var _profileCompleted: Boolean = false,
    private var _deletedAt: OffsetDateTime? = null,
) {
    val nextSequenceId get() = _sequenceId.next()
    val isProfileCompleted get() = _profileCompleted
    val isAdmin get() = _roles.contains(ROLE_ADMIN)
    val isDeleted get() = _deletedAt != null
    val deletedAt get() = _deletedAt

    fun canEdit(author: Author) = when (author) {
        is Author.Connected -> author.userId == id
        is Author.Unlogged -> false
    }

    private fun apply(event: ProfileCompleted): UserDecision {
        _profileCompleted = true
        _roles = Roles(setOf(ROLE_USER))
        return applyDefault(event)
    }

    private fun apply(event: PromotedAsAdmin): UserDecision {
        _roles = _roles + ROLE_ADMIN
        return applyDefault(event)
    }

    private fun apply(event: Deleted): UserDecision {
        _deletedAt = event.date
        return applyDefault(event)
    }

    private fun apply(event: Reactivated): UserDecision {
        _deletedAt = null
        return applyDefault(event)
    }

    private fun applyDefault(event: UserEvent): UserDecision {
        _sequenceId = event.sequenceId
        return this
    }

    companion object {
        fun from(history: UserHistory): UserDecision {
            val reducer = EventsReducer<UserDecision, UserEvent, Initialized>(
                factory = UserDecision::applyInitialization,
                defaultHandler = UserDecision::applyDefault,
            )
                .register<ProfileCompleted>(UserDecision::apply)
                .register<PromotedAsAdmin>(UserDecision::apply)
                .register<Deleted>(UserDecision::apply)
                .register<Reactivated>(UserDecision::apply)

            return reducer.reduce(history)
        }

        private fun applyInitialization(event: Initialized) = UserDecision(
            id = event.userId,
            email = event.email,
            _sequenceId = event.sequenceId,
            _roles = event.roles,
        )
    }
}
