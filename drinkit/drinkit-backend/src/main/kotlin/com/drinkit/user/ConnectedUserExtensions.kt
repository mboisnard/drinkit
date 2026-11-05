package com.drinkit.user

import com.drinkit.security.ConnectedUser
import com.drinkit.user.core.Author
import com.drinkit.user.core.UserId

val ConnectedUser.userId get() = UserId(id)

val ConnectedUser.author get() = Author.Connected(userId)
