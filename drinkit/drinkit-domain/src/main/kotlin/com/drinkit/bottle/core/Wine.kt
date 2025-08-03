package com.drinkit.bottle.core

enum class WineColor {
    RED, WHITE, ROSE, ORANGE, CLAIRET
}

data class Wine(
    override val id: BottleId,
    override val alcoholContent: AlcoholContent,
    override val description: Description?,
    override val name: BottleName,
    override val price: Price?,
    val color: WineColor,
    val vintage: Vintage,
    val grapes: Grapes,
    val winery: WineryId?,
): Bottle {

    fun validate() = buildList {
        addAll(alcoholContent.validate())
        description?.let { addAll(description.validate()) }
        price?.let { addAll(price.validate()) }
    }
}