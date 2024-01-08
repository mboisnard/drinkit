package com.drinkit.domain.common

data class Point(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180)
    }
}

data class Country(
    val code: String,
    val name: String,
) {
    init {
        require(code.length == 2)
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
