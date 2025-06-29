package com.drinkit.user.core

import com.drinkit.documentation.event.sourcing.Aggregate
import com.drinkit.event.sourcing.EventsReducer
import com.drinkit.event.sourcing.SequenceId

@Aggregate
data class UserDecision(
    private var sequenceId: SequenceId,
    private var roles: Roles,
    private var profileCompleted: Boolean = false
) {
    val nextSequenceId get() = sequenceId.next()

    companion object {
        fun from(history: UserHistory): UserDecision {
            val reducer = EventsReducer<UserDecision, UserEvent, UserInitialized>(
                factory = UserDecision::applyInitialization
            )

            return reducer.reduce(history.initEvent, history.remainingEvents)
        }

        private fun applyInitialization(event: UserInitialized) = UserDecision(
            sequenceId = event.sequenceId,
            roles = event.roles,
        )
    }
}
