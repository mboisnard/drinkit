package com.drinkit.common

import com.drinkit.identifier.AbstractId
import com.drinkit.identifier.IdFactory

/** The browser of someone not signed in, which the security starter keeps from one visit to the next. */
class VisitorId(value: String) : AbstractId(value) {
    companion object : IdFactory<VisitorId>(::VisitorId)
}
