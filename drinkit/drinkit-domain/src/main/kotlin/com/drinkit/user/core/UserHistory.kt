package com.drinkit.user.core

import com.drinkit.common.Author
import com.drinkit.event.sourcing.DomainEvent
import com.drinkit.event.sourcing.History
import com.drinkit.event.sourcing.SequenceId
import java.time.OffsetDateTime
import java.util.Locale

typealias UserHistory = History<UserEvent, Initialized>

sealed interface UserEvent : DomainEvent {
    val userId: UserId
    val date: OffsetDateTime
    val author: Author
}

data class Initialized(
    override val userId: UserId,
    override val sequenceId: SequenceId,
    override val date: OffsetDateTime,
    override val author: Author,
    val email: Email,
    val password: EncodedPassword,
    val roles: Roles,
    val preferredLocale: Locale,
) : UserEvent

data class ProfileCompleted(
    override val userId: UserId,
    override val date: OffsetDateTime,
    override val author: Author.Connected,
    override val sequenceId: SequenceId,
    val profile: ProfileInformation,
) : UserEvent

data class Verified(
    override val userId: UserId,
    override val date: OffsetDateTime,
    override val author: Author.Connected,
    override val sequenceId: SequenceId,
) : UserEvent

data class PromotedAsAdmin(
    override val userId: UserId,
    override val date: OffsetDateTime,
    override val author: Author.Connected,
    override val sequenceId: SequenceId,
) : UserEvent

data class Deleted(
    override val userId: UserId,
    override val date: OffsetDateTime,
    override val author: Author.Connected,
    override val sequenceId: SequenceId,
) : UserEvent