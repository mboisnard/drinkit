package com.drinkit.user.registration

import com.drinkit.user.core.Email
import com.drinkit.user.core.UserId

sealed interface User {
    val id: UserId
    val email: Email
}

