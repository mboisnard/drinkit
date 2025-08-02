package com.drinkit.user.spi

import com.drinkit.user.core.Initialized
import com.drinkit.user.core.User
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId

class InMemoryUserEventsStore(
    private val users: Users,
): UserEvents {

    private val eventsStore: MutableMap<UserId, UserHistory> = mutableMapOf()

    override fun findAllBy(userId: UserId): UserHistory? =
        eventsStore[userId]?.let { history ->
            history.copy(remainingEvents = history.remainingEvents.sortedBy { it.sequenceId })
        }

    override fun save(event: UserEvent): User {
        val userId = event.userId

        if (eventsStore[userId] == null && event is Initialized) {
            eventsStore[userId] = UserHistory(event)
        } else {
            eventsStore.computeIfPresent(userId) { _, history ->
                history.add(event)
            }
        }

        val user = findAllBy(userId)!!.let { history -> User.Companion.from(history) }

        return users.saveOrUpdate(user)
    }

    private fun UserHistory.add(event: UserEvent) = this.copy(
        remainingEvents = this.remainingEvents + event,
    )
}