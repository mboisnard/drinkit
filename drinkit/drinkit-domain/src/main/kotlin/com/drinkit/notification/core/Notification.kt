package com.drinkit.notification.core

import com.drinkit.documentation.clean.architecture.CoreDomain
import java.util.Locale

/** What to tell [recipient], translated in [locale] only by the channel that sends it. */
@CoreDomain
data class Notification(val content: NotificationContent, val locale: Locale, val recipient: Recipient)

data class Recipient(val value: String)

/** Keys of the translation catalog, and the values their messages place by name. */
data class NotificationContent(val titleKey: String, val contentKey: String, val values: Map<String, Any> = emptyMap())
