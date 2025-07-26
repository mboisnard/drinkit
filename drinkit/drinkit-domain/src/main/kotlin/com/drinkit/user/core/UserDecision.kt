package com.drinkit.user.core

import com.drinkit.common.Author
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.event.sourcing.EventsReducer
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.user.core.Roles.Role.ROLE_ADMIN
import com.drinkit.user.core.Roles.Role.ROLE_USER

@Aggregate
data class UserDecision(
    val id: UserId,
    val email: Email,
    private var _sequenceId: SequenceId,
    private var _roles: Roles,
    private var _profileCompleted: Boolean = false,
    private var _verified: Boolean = false,
    private var _deleted: Boolean = false,
) {
    val nextSequenceId get() = _sequenceId.next()
    val isProfileCompleted get() = _profileCompleted
    val isAdmin get() = _roles.contains(ROLE_ADMIN)
    val isVerified get() = _verified
    val isDeleted get() = _deleted

    fun canEdit(author: Author) = when (author) {
        is Author.Connected -> author.userId == id
        is Author.Unlogged -> false
    }

    private fun apply(event: ProfileCompleted): UserDecision {
        _profileCompleted = true
        _roles = Roles(setOf(ROLE_USER))
        return applyDefault(event)
    }

    private fun apply(event: Verified): UserDecision {
        _verified = true
        return applyDefault(event)
    }

    private fun apply(event: PromotedAsAdmin): UserDecision {
        _roles = _roles + ROLE_ADMIN
        return applyDefault(event)
    }

    private fun apply(event: Deleted): UserDecision {
        _deleted = true
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
                .register<Verified>(UserDecision::apply)
                .register<PromotedAsAdmin>(UserDecision::apply)
                .register<Deleted>(UserDecision::apply)

            return reducer.reduce(history.initEvent, history.remainingEvents)
        }

        private fun applyInitialization(event: Initialized) = UserDecision(
            id = event.userId,
            email = event.email,
            _sequenceId = event.sequenceId,
            _roles = event.roles,
        )
    }
}