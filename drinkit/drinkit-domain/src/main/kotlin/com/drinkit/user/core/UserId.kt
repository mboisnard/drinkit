package com.drinkit.user.core

import com.drinkit.common.AbstractId

data class UserId(
    override val value: String,
) : AbstractId(value)