package com.drinkit.search.engine

import java.math.BigDecimal
import java.time.LocalDate

internal data class Wine(
    val id: String,
    val name: String,
    val price: BigDecimal? = null,
    val bottled: LocalDate? = null,
    val grapes: List<String> = emptyList(),
)
