package com.drinkit.common

import java.util.UUID

data class CorrelationId(val value: UUID) {

    companion object {
        fun create() = CorrelationId(UUID.randomUUID())
    }
}
