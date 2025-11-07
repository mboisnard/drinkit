package com.drinkit.bottle.core

import com.drinkit.common.AbstractId
import com.drinkit.common.Percentage
import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.utils.addIfNotMatch
import com.drinkit.utils.doesntContainsInvisibleCharacters
import java.math.BigDecimal
import java.time.Year

data class BottleId(override val value: String): AbstractId(value)

data class Vintage(val value: Year)

data class BottleName(val value: String)

// Also named Alcohol By Volume (abv)
data class AlcoholContent(val value: Percentage) {
    fun validate() = buildList {
        addAll(value.validate())
    }
}

data class Price(val value: BigDecimal) {
    fun validate() = buildList {
        addIfNotMatch(value >= BigDecimal.ZERO, "Price must be positive")
    }
}

data class Description(val value: String) {
    fun validate() = buildList {
        addIfNotMatch(value.isNotBlank(), "Description must not be blank")
        addIfNotMatch(
            value.doesntContainsInvisibleCharacters(),
            "Description should not contains invisible characters, $value"
        )
    }
}

@CoreDomain
sealed interface Bottle {
    val id: BottleId
    val alcoholContent: AlcoholContent
    val description: Description?
    val name: BottleName
    val price: Price?
}