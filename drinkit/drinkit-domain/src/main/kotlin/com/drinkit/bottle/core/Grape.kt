package com.drinkit.bottle.core

import com.drinkit.common.HumanId
import com.drinkit.common.Percentage

data class GrapeId(
    val value: HumanId,
)

// Cépage in French
data class Grape(
    val id: GrapeId,
    val percentage: Percentage,
)

data class Grapes(val values: List<Grape>)