package com.drinkit.user.core

import com.drinkit.common.Constants.MAX_FIRSTNAME_LENGTH
import com.drinkit.common.Constants.MAX_LASTNAME_LENGTH
import com.drinkit.common.Constants.MIN_BIRTH_DATE
import com.drinkit.common.Constants.MIN_FIRSTNAME_LENGTH
import com.drinkit.common.Constants.MIN_LASTNAME_LENGTH
import com.drinkit.kotlin.addUnless
import com.drinkit.kotlin.containsNoInvisibleCharacter
import com.drinkit.kotlin.hasLengthBetween
import com.drinkit.kotlin.isStrictlyBetween
import java.time.Clock
import java.time.LocalDate

data class ProfileInformation(val firstName: FirstName, val lastName: LastName, val birthDate: BirthDate?) {
    fun validate() = buildList {
        addAll(firstName.validate())
        addAll(lastName.validate())
        birthDate?.let { addAll(birthDate.validate()) }
    }
}

data class FirstName(val value: String) {
    fun validate() = buildList {
        addUnless(value.isNotBlank(), "FirstName should not be blank")
        addUnless(
            value.containsNoInvisibleCharacter(),
            "FirstName should not contains invisible characters, $value",
        )
        addUnless(
            value.hasLengthBetween(MIN_FIRSTNAME_LENGTH, MAX_FIRSTNAME_LENGTH),
            "FirstName should have size between $MIN_FIRSTNAME_LENGTH and $MAX_FIRSTNAME_LENGTH, $value",
        )
    }
}

data class LastName(val value: String) {
    fun validate() = buildList {
        addUnless(value.isNotBlank(), "LastName should not be blank")
        addUnless(
            value.containsNoInvisibleCharacter(),
            "LastName should not contains invisible characters, $value",
        )
        addUnless(
            value.hasLengthBetween(MIN_LASTNAME_LENGTH, MAX_LASTNAME_LENGTH),
            "LastName should have size between $MIN_LASTNAME_LENGTH and $MAX_LASTNAME_LENGTH, $value",
        )
    }
}

data class BirthDate(val value: LocalDate) {
    fun validate() = buildList {
        addUnless(
            value.isStrictlyBetween(MIN_BIRTH_DATE, LocalDate.now(Clock.systemUTC())),
            "Birthdate should be between $MIN_BIRTH_DATE and now, $value",
        )
    }
}
