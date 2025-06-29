package com.drinkit.common

import com.drinkit.user.core.UserId

sealed interface Author {
    data class Connected(val userId: UserId) : Author

    data class Unlogged(val correlationId: CorrelationId) : Author

    fun connectedAuthorOrFail(): Connected = when (this) {
        is Connected -> this
        else -> throw IllegalStateException("Anonymous author is not allowed")
    }
}