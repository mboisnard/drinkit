package com.drinkit.user.core

import com.drinkit.common.VisitorId

sealed interface Author {
    data class Connected(val userId: UserId) : Author

    data class Unlogged(val visitorId: VisitorId) : Author

    fun connectedAuthorOrFail(): Connected = when (this) {
        is Connected -> this
        else -> error("Anonymous author is not allowed")
    }
}
