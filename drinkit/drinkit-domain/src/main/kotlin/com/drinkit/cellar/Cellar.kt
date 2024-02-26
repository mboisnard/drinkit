package com.drinkit.cellar

import com.drinkit.common.*
import com.drinkit.common.Constants.MAX_CELLAR_NAME_LENGTH
import com.drinkit.user.CompletedUser
import com.drinkit.user.UserId
import com.drinkit.utils.doesntContainsInvisibleCharacters
import com.drinkit.utils.hasMinLength

data class CellarId(
    override val value: String,
): AbstractId(value) {
    init {
        require(value.isId())
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
                && value.doesntContainsInvisibleCharacters()
                && value.hasMinLength(MAX_CELLAR_NAME_LENGTH)) {
            "Cellar name should not be blank, contains invisible chars or have less " +
                    "than $MAX_CELLAR_NAME_LENGTH characters. Given value: $value"
        }
    }
}

data class Cellar(
    val id: CellarId,
    val name: CellarName,
    val location: CityLocation,
    val owner: UserId,
) {

    fun canBeSeenBy(user: CompletedUser): Boolean =
        user.id == owner || user.isAdmin
}
