package com.drinkit.cellar

import com.drinkit.common.CityLocation
import com.drinkit.common.Constants.MAX_CELLAR_NAME_LENGTH
import com.drinkit.user.UserId
import org.bson.types.ObjectId

data class CellarId(
    val value: ObjectId,
) {
    companion object {
        fun create() = CellarId(value = ObjectId())

        fun from(value: String) = CellarId(value = ObjectId(value))
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
