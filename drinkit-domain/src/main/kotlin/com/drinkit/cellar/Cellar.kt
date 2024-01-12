package com.drinkit.cellar

import com.drinkit.common.AbstractId
import com.drinkit.common.CityLocation
import com.drinkit.common.Constants
import com.drinkit.common.Constants.ID_REGEX
import com.drinkit.common.Constants.MAX_CELLAR_NAME_LENGTH
import com.drinkit.common.IdGenerator
import com.drinkit.user.UserId

data class CellarId(
    override val value: String,
): AbstractId(value) {
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
        require(value.isNotBlank()
                && !Constants.INVISIBLE_CHARS_REGEX.containsMatchIn(value)
                && value.length <= MAX_CELLAR_NAME_LENGTH) {
            "Cellar name should not be blank, contains invisible chars or have less than $MAX_CELLAR_NAME_LENGTH characters"
        }
    }
}

data class Cellar(
    val id: CellarId,
    val name: CellarName,
    val location: CityLocation,
    val owner: UserId,
)
