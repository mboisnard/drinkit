package com.drinkit.user.core

import com.drinkit.common.Constants
import com.drinkit.utils.doesntContainsInvisibleCharacters
import com.drinkit.utils.hasLengthBetween
import com.drinkit.utils.isBetween
import java.time.Clock
import java.time.LocalDate

data class ProfileInformation(
    val firstName: FirstName,
    val lastName: LastName,
    val birthDate: BirthDate?,
)

data class FirstName(
    val value: String,
) {
    init {
        require(
            value.isNotBlank() &&
                value.doesntContainsInvisibleCharacters() &&
                value.hasLengthBetween(Constants.MIN_FIRSTNAME_LENGTH, Constants.MAX_FIRSTNAME_LENGTH)
        ) {
            "Invalid FirstName format (should not be blank, should not contains invisible characters, " +
                "should have size between ${Constants.MIN_FIRSTNAME_LENGTH} and ${Constants.MAX_FIRSTNAME_LENGTH}. Given value: $value"
        }
    }
}

data class LastName(
    val value: String,
) {
    init {
        require(
            value.isNotBlank() &&
                value.doesntContainsInvisibleCharacters() &&
                value.hasLengthBetween(Constants.MIN_LASTNAME_LENGTH, Constants.MAX_LASTNAME_LENGTH)
        ) {
            "Invalid LastName format (should not be blank, should not contains invisible characters, " +
                "should have size between ${Constants.MIN_LASTNAME_LENGTH} and ${Constants.MAX_LASTNAME_LENGTH}. Given value: $value"
        }
    }
}

data class BirthDate(
        val value: LocalDate,
) {
    init {
        require(value.isBetween(Constants.MIN_BIRTH_DATE, LocalDate.now(Clock.systemUTC()))) {
            "Birthdate should be between ${Constants.MIN_BIRTH_DATE} and now. Given value: $value"
        }
    }
}