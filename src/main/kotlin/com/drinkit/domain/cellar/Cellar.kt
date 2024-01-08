package com.drinkit.domain.cellar

import com.drinkit.domain.common.CityLocation
import com.drinkit.domain.user.UserId
import org.bson.types.ObjectId

data class CellarId(
    val value: ObjectId,
) {
    companion object {
        fun create() = CellarId(value = ObjectId())

        fun from(value: String) = CellarId(value = ObjectId(value))
    }
}

data class Cellar(
    val id: CellarId,
    val name: String,
    val location: CityLocation,
    val owner: UserId,
)
