package com.drinkit.user.spi

import com.drinkit.messaging.PlatformEvent
import com.drinkit.user.core.UserId
import java.util.Locale

data class UserCreated(
    val userId: UserId,
    val locale: Locale,
) : PlatformEvent<UserCreated>