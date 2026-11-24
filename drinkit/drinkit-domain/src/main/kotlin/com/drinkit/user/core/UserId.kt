package com.drinkit.user.core

import com.drinkit.identifier.AbstractId
import com.drinkit.identifier.IdFactory

class UserId(value: String) : AbstractId(value) {
    companion object : IdFactory<UserId>(::UserId)
}
