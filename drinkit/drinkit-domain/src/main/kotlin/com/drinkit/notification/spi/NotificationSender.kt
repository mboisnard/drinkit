package com.drinkit.notification.spi

import com.drinkit.notification.core.Notification

fun interface NotificationSender {

    fun send(notification: Notification): Boolean
}
