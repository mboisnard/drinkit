package com.drinkit.bottle.core

import com.drinkit.utils.addIfNotMatch

data class InternationalBitternessUnit(val value: Int) {
    companion object {
        private const val IBU_MIN_VALUE = 1
        private const val IBU_MAX_VALUE = 150
    }

    fun validate() = buildList {
        addIfNotMatch(
            value in IBU_MIN_VALUE..IBU_MAX_VALUE,
            "Bitterness must be between $IBU_MIN_VALUE & $IBU_MAX_VALUE, value: $value",
        )
    }
}

data class Beer(
    override val id: BottleId,
    override val alcoholContent: AlcoholContent,
    override val description: Description?,
    override val name: BottleName,
    override val price: Price?,
    val ibu: InternationalBitternessUnit,
) : Bottle {

    fun validate() = buildList {
        addAll(alcoholContent.validate())
        description?.let { addAll(description.validate()) }
        price?.let { addAll(price.validate()) }
        addAll(ibu.validate())
    }
}
