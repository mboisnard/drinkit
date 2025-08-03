package com.drinkit.bottle.core

import com.drinkit.common.FullLocation
import com.drinkit.common.HumanId

data class WineryId(
    val value: HumanId,
)

data class WineryName(val value: String)

data class Winery(
    val id: WineryId,
    val name: WineryName,
    val location: FullLocation,
)
