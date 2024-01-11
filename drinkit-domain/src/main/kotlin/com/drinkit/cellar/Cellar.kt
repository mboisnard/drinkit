package com.drinkit.cellar

import com.drinkit.common.CityLocation
import com.drinkit.common.Constants.ID_REGEX
import com.drinkit.common.Constants.MAX_CELLAR_NAME_LENGTH
import com.drinkit.common.IdGenerator
import com.drinkit.user.UserId

data class CellarId(
    val value: String,
) {
    init {
        require(ID_REGEX.matches(value))
    }

    companion object {
        fun create(generator: IdGenerator) = CellarId(value = generator.createNewId())
    }
}

data class CellarName(
    val value: String,
) {
    init {
        require(value.isNotBlank() && value.length <= MAX_CELLAR_NAME_LENGTH)
    }
}

data class Cellar(
    val id: CellarId,
    val name: CellarName,
    val location: CityLocation,
    val owner: UserId,
)
