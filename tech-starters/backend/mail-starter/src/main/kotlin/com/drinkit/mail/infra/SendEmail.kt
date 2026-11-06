package com.drinkit.mail.infra

import com.drinkit.mail.Email
import com.drinkit.messaging.PlatformEvent

// Not a data class: its toString would print the recipients and the content
internal class SendEmail(val email: Email) : PlatformEvent<SendEmail>
