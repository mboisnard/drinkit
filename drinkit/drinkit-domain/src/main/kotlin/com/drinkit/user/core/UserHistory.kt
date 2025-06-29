package com.drinkit.user.core

import com.drinkit.common.Author
import com.drinkit.event.sourcing.DomainEvent
import com.drinkit.event.sourcing.History
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.user.EncodedPassword
import java.time.OffsetDateTime

typealias UserHistory = History<UserEvent, UserInitialized>

sealed interface UserEvent : DomainEvent {
    val userId: UserId
    val date: OffsetDateTime
    val author: Author
}

data class UserInitialized(
    override val userId: UserId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
    override val author: Author,
    val email: Email,
    val password: EncodedPassword,
    val roles: Roles,
) : UserEvent