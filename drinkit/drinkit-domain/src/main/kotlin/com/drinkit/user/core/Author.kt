package com.drinkit.user.core

import com.drinkit.common.CorrelationId

sealed interface Author {
    data class Connected(val userId: UserId) : Author

    data class Unlogged(val correlationId: CorrelationId) : Author

    fun connectedAuthorOrFail(): Connected = when (this) {
        is Connected -> this
        else -> error("Anonymous author is not allowed")
    }
}
