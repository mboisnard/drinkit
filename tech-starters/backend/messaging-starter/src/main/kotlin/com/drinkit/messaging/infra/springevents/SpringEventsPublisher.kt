package com.drinkit.messaging.infra.springevents

import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED
import org.springframework.transaction.support.TransactionSynchronizationManager

internal class SpringEventsPublisher(private val applicationEventPublisher: ApplicationEventPublisher) :
    PlatformEventPublisher {

    override fun <EventType : PlatformEvent<EventType>> publish(event: EventType): EventType {
        // Its key now, while the key of the request or the event being handled is the current one
        val delivery = Delivery.of(event)

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(PublishOnCommit(delivery))
        } else {
            applicationEventPublisher.publishEvent(delivery)
        }

        return event
    }

    // Not afterCommit: Spring never calls one registered from another synchronization's afterCommit.
    // Not a data class: the synchronizations are a set, which would merge two equal events
    private inner class PublishOnCommit(private val delivery: Delivery) : TransactionSynchronization {

        override fun afterCompletion(status: Int) {
            if (status == STATUS_COMMITTED) applicationEventPublisher.publishEvent(delivery)
        }
    }
}
