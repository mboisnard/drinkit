package com.drinkit.messaging.infra.springevents

import com.drinkit.messaging.PlatformEvent

internal data class SomethingHappened(val id: Int) : PlatformEvent<SomethingHappened>

internal data class SomethingElseHappened(val id: Int) : PlatformEvent<SomethingElseHappened>
