package com.drinkit.notification.spi

import com.drinkit.notification.core.Notification

class SpyNotificationSender : NotificationSender {

    private var sentNotifications: List<Notification> = mutableListOf()

    override fun queue(notification: Notification) {
        sentNotifications += notification
    }

    fun count(): Int = sentNotifications.size

    fun lastSent(): Notification? = sentNotifications.lastOrNull()
}
