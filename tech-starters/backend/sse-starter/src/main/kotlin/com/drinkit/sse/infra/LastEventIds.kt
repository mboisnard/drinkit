package com.drinkit.sse.infra

import org.springframework.cache.Cache
import java.util.UUID

internal const val LAST_EVENT_IDS_CACHE = "sse-last-event-ids"

/**
 * The id of the last event sent to each recipient, which a stream gives the tab at its opening: a tab whose last id
 * differs missed an event while its stream was closed. Every instance keeps it, as each one gets every event.
 */
internal class LastEventIds(private val cache: Cache) {

    fun sent(recipient: Recipient, eventId: String) = cache.put(recipient, eventId)

    // A recipient no event reached yet gets an id of its own, kept until an event replaces it. An entry the cache lost
    // gives a new id, so the tab reads its data again rather than miss an event
    fun lastOf(recipient: Recipient): String {
        val opening = UUID.randomUUID().toString()

        return cache.putIfAbsent(recipient, opening)?.get() as String? ?: opening
    }
}
