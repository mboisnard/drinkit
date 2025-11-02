package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.event.sourcing.SequenceId
import com.drinkit.generated.jooq.tables.records.UserEventRecord
import com.drinkit.postgresql.jooq.JSONBToJacksonConverter
import com.drinkit.user.UserEventPayloadType.DELETED
import com.drinkit.user.UserEventPayloadType.INITIALIZED
import com.drinkit.user.UserEventPayloadType.PROFILE_COMPLETED
import com.drinkit.user.UserEventPayloadType.PROMOTED_AS_ADMIN
import com.drinkit.user.UserEventPayloadType.VERIFIED
import com.drinkit.user.core.Deleted
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.Initialized
import com.drinkit.user.core.ProfileCompleted
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.PromotedAsAdmin
import com.drinkit.user.core.Roles
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserId
import com.drinkit.user.core.Verified
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.JSONB
import java.time.OffsetDateTime
import java.util.Locale

internal fun UserEvent.toEventNameWithPayload(objectMapper: ObjectMapper): Pair<UserEventPayloadType, JSONB> {
    val payload = when (this) {
        is Initialized -> INITIALIZED to InitializedPayload.from(this)
        is ProfileCompleted -> PROFILE_COMPLETED to ProfileCompletedPayload.from(this)
        is Verified -> VERIFIED to NoPayload
        is PromotedAsAdmin -> PROMOTED_AS_ADMIN to NoPayload
        is Deleted -> DELETED to NoPayload
    }

    return payload.first to JSONBToJacksonConverter(payload.second.javaClass, objectMapper).to(payload.second)
}

internal fun UserEventRecord.toEvent(objectMapper: ObjectMapper): UserEvent {
    val commonFields = UserEventCommonFields.from(this, objectMapper)

    return when (UserEventPayloadType.valueOf(eventName)) {
        INITIALIZED -> {
            val payload = JSONBToJacksonConverter(InitializedPayload::class.java, objectMapper).from(payload)
            Initialized(
                userId = commonFields.userId,
                sequenceId = commonFields.sequenceId,
                date = commonFields.date,
                author = commonFields.author,
                email = payload.email,
                password = payload.password,
                roles = payload.roles,
                preferredLocale = payload.preferredLocale,
            )
        }
        PROFILE_COMPLETED -> {
            val payload = JSONBToJacksonConverter(ProfileCompletedPayload::class.java, objectMapper).from(payload)
            ProfileCompleted(
                userId = commonFields.userId,
                sequenceId = commonFields.sequenceId,
                date = commonFields.date,
                author = commonFields.connectedAuthor,
                profile = payload.profile,
            )
        }
        VERIFIED -> {
            Verified(
                userId = commonFields.userId,
                sequenceId = commonFields.sequenceId,
                date = commonFields.date,
                author = commonFields.connectedAuthor,
            )
        }
        PROMOTED_AS_ADMIN -> {
            PromotedAsAdmin(
                userId = commonFields.userId,
                sequenceId = commonFields.sequenceId,
                date = commonFields.date,
                author = commonFields.connectedAuthor,
            )
        }
        DELETED -> {
            Deleted(
                userId = commonFields.userId,
                sequenceId = commonFields.sequenceId,
                date = commonFields.date,
                author = commonFields.connectedAuthor,
            )
        }
    }
}

internal enum class UserEventPayloadType {
    INITIALIZED,
    PROFILE_COMPLETED,
    VERIFIED,
    PROMOTED_AS_ADMIN,
    DELETED
}

internal sealed interface UserEventPayload

internal object NoPayload : UserEventPayload

internal data class InitializedPayload(
    val email: Email,
    val password: EncodedPassword,
    val roles: Roles,
    val preferredLocale: Locale,
) : UserEventPayload {

    companion object {
        fun from(event: Initialized) = InitializedPayload(
            email = event.email,
            password = event.password,
            roles = event.roles,
            preferredLocale = event.preferredLocale,
        )
    }
}

internal data class ProfileCompletedPayload(
    val profile: ProfileInformation,
) : UserEventPayload {

    companion object {
        fun from(event: ProfileCompleted) = ProfileCompletedPayload(
            profile = event.profile
        )
    }
}

internal data class UserEventCommonFields(
    val userId: UserId,
    val sequenceId: SequenceId,
    val date: OffsetDateTime,
    val author: Author,
) {
    val connectedAuthor get(): Author.Connected = when (author) {
        is Author.Connected -> author
        is Author.Unlogged -> throw IllegalStateException("Can't be unlogged here")
    }

    companion object {
        fun from(record: UserEventRecord, objectMapper: ObjectMapper) = UserEventCommonFields(
            userId = UserId(record.userId),
            sequenceId = SequenceId(record.sequenceId),
            date = record.date,
            author = JSONBToJacksonConverter(Author::class.java, objectMapper).from(record.author),
        )
    }
}