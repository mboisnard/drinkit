package com.drinkit.user.spi

import com.drinkit.user.core.User
import com.drinkit.user.core.UserEvent
import com.drinkit.user.core.UserHistory
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserInitialized
import org.springframework.stereotype.Repository
import kotlin.collections.set

// Should be in testFixtures after implementing the Jooq Repository
@Repository
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

        if (eventsStore[userId] == null && event is UserInitialized) {
            eventsStore[userId] = UserHistory(event)
        } else {
            eventsStore.computeIfPresent(userId) { _, history ->
                history.add(event)
            }
        }

        val user = findAllBy(userId)!!.let { history -> User.from(history) }

        return users.saveOrUpdate(user)
    }

    private fun UserHistory.add(event: UserEvent) = this.copy(
        remainingEvents = this.remainingEvents + event,
    )
}