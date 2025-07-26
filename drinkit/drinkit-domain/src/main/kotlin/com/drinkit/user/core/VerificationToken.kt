package com.drinkit.user.core

import java.time.OffsetDateTime

data class VerificationToken(
    val userId: UserId,
    val value: String,
    val expiryDate: OffsetDateTime,
)