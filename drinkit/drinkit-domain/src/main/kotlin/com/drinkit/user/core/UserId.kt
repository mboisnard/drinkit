package com.drinkit.user.core

import com.drinkit.common.AbstractId
import com.drinkit.common.IdGenerator

data class UserId(
    override val value: String,
) : AbstractId(value) {

    companion object {
        fun create(generator: IdGenerator) = UserId(value = generator.createNewId())
    }
}