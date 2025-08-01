package com.drinkit.user

import com.drinkit.user.core.User
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId
import com.drinkit.user.spi.UserEvents
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
internal class JooqUserEventsRepository(
    private val dsl: DSLContext,
) : UserEvents {

    override fun findAllBy(userId: UserId): UserHistory? {
        TODO("Not yet implemented")
    }

    override fun save(event: UserEvent): User {
        TODO("Not yet implemented")
    }
}