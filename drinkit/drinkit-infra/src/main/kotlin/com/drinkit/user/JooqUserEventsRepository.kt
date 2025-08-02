package com.drinkit.user

import com.drinkit.common.Author
import com.drinkit.event.sourcing.transaction.DuplicateSequenceException
import com.drinkit.generated.jooq.keys.USER_EVENT_PKEY
import com.drinkit.generated.jooq.tables.references.USER_EVENT
import com.drinkit.jooq.JSONBToJacksonConverter
import com.drinkit.jooq.fetchSequence
import com.drinkit.jooq.isEventSourcingSequenceException
import com.drinkit.user.core.Initialized
import com.drinkit.user.core.User
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.UserEvents
import com.drinkit.user.spi.Users
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Repository

@Repository
internal class JooqUserEventsRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
    private val users: Users,
) : UserEvents {

    private val authorConverter = JSONBToJacksonConverter(Author::class.java, objectMapper)

    override fun findAllBy(userId: UserId): UserHistory? {
        return dsl.selectFrom(USER_EVENT)
            .where(USER_EVENT.USER_ID.eq(userId.value))
            .orderBy(USER_EVENT.SEQUENCE_ID.asc())
            .fetchSequence { it.toEvent(objectMapper) }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { UserHistory.from<UserEvent, Initialized>(it) }
    }

    override fun save(event: UserEvent): User {
        val (eventName, payload) = event.toEventNameWithPayload(objectMapper)

        try {
            dsl.insertInto(USER_EVENT)
                .set(USER_EVENT.USER_ID, event.userId.value)
                .set(USER_EVENT.SEQUENCE_ID, event.sequenceId.value)
                .set(USER_EVENT.EVENT_NAME, eventName.name)
                .set(USER_EVENT.DATE, event.date)
                .set(USER_EVENT.AUTHOR, authorConverter.to(event.author))
                .set(USER_EVENT.PAYLOAD, payload)
                .execute()
        } catch (ex: DataAccessException) {
            if (ex.isEventSourcingSequenceException(USER_EVENT_PKEY.name)) {
                throw DuplicateSequenceException(
                    "${event.sequenceId} already exists for ${event.userId} when trying to save $event",
                    ex,
                )
            }

            throw ex
        }

        val user = findAllBy(event.userId)!!.let { User.from(it) }

        return users.saveOrUpdate(user)
    }
}