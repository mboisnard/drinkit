package com.drinkit.cellar.core

import com.drinkit.common.AbstractId
import com.drinkit.common.CityLocation
import com.drinkit.common.Constants.MAX_CELLAR_NAME_LENGTH
import com.drinkit.common.Constants.MAX_CELLAR_ROOM_LENGTH
import com.drinkit.common.isId
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.utils.addIfNotMatch
import com.drinkit.utils.doesntContainsInvisibleCharacters
import com.drinkit.utils.hasLengthBetween

data class CellarId(
    override val value: String,
) : AbstractId(value) {
    init {
        require(value.isId())
    }
}

data class CellarName(
    val value: String,
) {
    fun validate() = buildList {
        addIfNotMatch(value.isNotBlank(), "Cellar name should not be blank")
        addIfNotMatch(
            value.doesntContainsInvisibleCharacters(),
            "Cellar name should not contains invisible characters, $value"
        )
        addIfNotMatch(
            value.hasLengthBetween(1, MAX_CELLAR_NAME_LENGTH),
            "Cellar name should have size between 1 and $MAX_CELLAR_NAME_LENGTH, $value"
        )
    }
}

data class CellarRooms(
    val values: Set<CellarRoom>,
) {
    data class CellarRoom(
        val name: String,
    ) {
        init {
            require(
                name.isNotBlank() &&
                    name.doesntContainsInvisibleCharacters() &&
                    name.hasLengthBetween(1, MAX_CELLAR_ROOM_LENGTH)
            ) {
                "Cellar room should not be blank, contains invisible chars or have more " +
                    "than $MAX_CELLAR_ROOM_LENGTH characters. Given value: $name"
            }
        }
    }

    fun allAsString(): Set<String> = values.map { it.name }.toSet()

    companion object {
        val EMPTY = CellarRooms(emptySet())
    }
}

data class Cellar(
    val id: CellarId,
    val name: CellarName,
    val location: CityLocation,
    val rooms: CellarRooms,
    val owner: UserId,
) {

    fun canBeSeenBy(user: User): Boolean =
        user.id == owner
}
