package com.drinkit.user.core

import com.drinkit.common.Constants.MAX_PASSWORD_LENGTH
import com.drinkit.common.Constants.MIN_PASSWORD_LENGTH
import com.drinkit.kotlin.containsACapitalLetter
import com.drinkit.kotlin.containsANumber
import com.drinkit.kotlin.containsASpecialCharacter
import com.drinkit.kotlin.containsNoInvisibleCharacter
import com.drinkit.kotlin.hasLengthBetween

data class EncodedPassword(val value: String) {
    init {
        require(value.isNotBlank() && value.containsNoInvisibleCharacter()) {
            "Encoded password should not be blank or contains invisible characters. Given value: $value"
        }
    }

    companion object {
        fun from(password: Password, encoder: (String) -> String): EncodedPassword =
            EncodedPassword(encoder(password.value))
    }
}

data class Password(val value: String) {
    init {
        require(
            value.isNotBlank() &&
                value.hasLengthBetween(MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH) &&
                value.containsNoInvisibleCharacter() &&
                value.containsACapitalLetter() &&
                value.containsANumber() &&
                value.containsASpecialCharacter(),
        )
    }
}
