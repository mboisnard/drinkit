package com.drinkit.notification.core

import com.drinkit.documentation.clean.architecture.CoreDomain
import java.util.Locale

@CoreDomain
data class Notification(
    val content: NotificationContent? = null,
    val templateLocation: String? = null,
    val locale: Locale,
    val recipient: Recipient,
) {
    init {
        require(content != null || templateLocation != null)
    }
}

data class Recipient(val value: String)

data class NotificationContent(val title: String, val content: String)
