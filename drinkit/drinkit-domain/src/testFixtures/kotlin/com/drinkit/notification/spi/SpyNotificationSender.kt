package com.drinkit.notification.spi

import com.drinkit.notification.core.Notification

class SpyNotificationSender : NotificationSender {

    private var sentNotifications: List<Notification> = mutableListOf()

    override fun send(notification: Notification): Boolean {
        sentNotifications += notification
        return true
    }

    fun count(): Int = sentNotifications.size
}
