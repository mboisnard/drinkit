package com.drinkit.user.core

import com.drinkit.common.Author
import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.event.sourcing.EventsReducer
import com.drinkit.event.sourcing.SequenceId

@Aggregate
data class UserDecision(
    val id: UserId,
    private var _sequenceId: SequenceId,
    private var roles: Roles,
    private var _profileCompleted: Boolean = false
) {
    val nextSequenceId get() = _sequenceId.next()
    val isProfileCompleted get() = _profileCompleted

    fun canEdit(author: Author) = when (author) {
        is Author.Connected -> author.userId == id
        is Author.Unlogged -> false
    }

    private fun apply(event: ProfileCompleted): UserDecision {
        _profileCompleted = true
        return applyDefault(event)
    }

    private fun applyDefault(event: UserEvent): UserDecision {
        _sequenceId = event.sequenceId
        return this
    }

    companion object {
        fun from(history: UserHistory): UserDecision {
            val reducer = EventsReducer<UserDecision, UserEvent, UserInitialized>(
                factory = UserDecision::applyInitialization,
                defaultHandler = UserDecision::applyDefault,
            ).register<ProfileCompleted>(UserDecision::apply)

            return reducer.reduce(history.initEvent, history.remainingEvents)
        }

        private fun applyInitialization(event: UserInitialized) = UserDecision(
            id = event.userId,
            _sequenceId = event.sequenceId,
            roles = event.roles,
        )
    }
}