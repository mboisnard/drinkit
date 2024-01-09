package com.drinkit.common

import com.drinkit.common.Constants.COUNTRY_CODE_LENGTH
import com.drinkit.common.Constants.MAX_LATITUDE
import com.drinkit.common.Constants.MAX_LONGITUDE
import com.drinkit.common.Constants.MIN_LATITUDE
import com.drinkit.common.Constants.MIN_LONGITUDE

data class Point(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in MIN_LATITUDE.. MAX_LATITUDE && longitude in MIN_LONGITUDE..MAX_LONGITUDE)
    }
}

data class Country(
    val code: String,
    val name: String,
) {
    init {
        require(code.length == COUNTRY_CODE_LENGTH)
    }
}

data class CityLocation(
    val city: String,
    val country: Country,
    val point: Point?,
)

data class FullLocation(
    val street: String,
    val city: String,
    val country: Country,
    val point: Point?,
)
