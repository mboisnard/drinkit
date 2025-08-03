package com.drinkit.bottle.core

import com.drinkit.common.FullLocation
import com.drinkit.common.HumanId

data class BreweryId(val value: HumanId)

data class BreweryName(val value: String)

data class Brewery(
    val id: BreweryId,
    val name: BreweryName,
    val location: FullLocation,
)