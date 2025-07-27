package com.drinkit.user.core

import com.drinkit.common.Constants.MIN_PASSWORD_LENGTH
import com.drinkit.utils.containsACapitalLetter
import com.drinkit.utils.containsANumber
import com.drinkit.utils.containsASpecialCharacter
import com.drinkit.utils.doesntContainsInvisibleCharacters
import com.drinkit.utils.hasMinLength

class EncodedPassword(
    val value: String,
) {
    init {
        require(value.isNotBlank() && value.doesntContainsInvisibleCharacters()) {
            "Encoded password should not be blank or contains invisible characters. Given value: $value"
        }
    }

    companion object {
        fun from(password: Password, encoder: (String) -> String): EncodedPassword {
            return EncodedPassword(encoder(password.value))
        }
    }
}

data class Password(
    val value: String,
) {
    init {
        require(
            value.isNotBlank() &&
                value.hasMinLength(MIN_PASSWORD_LENGTH) &&
                value.doesntContainsInvisibleCharacters() &&
                value.containsACapitalLetter() &&
                value.containsANumber() &&
                value.containsASpecialCharacter()
        )
    }
}