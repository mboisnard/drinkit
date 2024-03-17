package com.drinkit.common

import com.drinkit.common.Constants.COUNTRY_CODE_LENGTH
import com.drinkit.common.Constants.MAX_LATITUDE
import com.drinkit.common.Constants.MAX_LONGITUDE
import com.drinkit.common.Constants.MIN_LATITUDE
import com.drinkit.common.Constants.MIN_LONGITUDE
import com.drinkit.utils.isBetween
import java.math.BigDecimal

data class Point(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
) {
    init {
        require(
            latitude.isBetween(MIN_LATITUDE, MAX_LATITUDE) &&
                longitude.isBetween(MIN_LONGITUDE, MAX_LONGITUDE)
        ) {
            "Invalid latitude or longitude. Given values: lat=$latitude, lon=$longitude"
        }
    }
}

data class Country(
    val code: String,
    val name: String,
) {
    init {
        require(code.length == COUNTRY_CODE_LENGTH) {
            "Country code must have $COUNTRY_CODE_LENGTH characters. Given value: $name"
        }
    }
}

data class CityLocation(
    val city: String,
    val country: Country,
    val point: Point,
)

data class FullLocation(
    val street: String,
    val city: String,
    val country: Country,
    val point: Point?,
)
