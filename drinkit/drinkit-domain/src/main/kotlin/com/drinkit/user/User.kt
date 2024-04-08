package com.drinkit.user

import com.drinkit.common.AbstractId
import com.drinkit.common.Constants.MAX_EMAIL_LENGTH
import com.drinkit.common.Constants.MAX_FIRSTNAME_LENGTH
import com.drinkit.common.Constants.MAX_LASTNAME_LENGTH
import com.drinkit.common.Constants.MIN_BIRTH_DATE
import com.drinkit.common.Constants.MIN_FIRSTNAME_LENGTH
import com.drinkit.common.Constants.MIN_LASTNAME_LENGTH
import com.drinkit.common.Constants.MIN_PASSWORD_LENGTH
import com.drinkit.common.IdGenerator
import com.drinkit.common.isId
import com.drinkit.utils.containsACapitalLetter
import com.drinkit.utils.containsANumber
import com.drinkit.utils.containsASpecialCharacter
import com.drinkit.utils.doesntContainsInvisibleCharacters
import com.drinkit.utils.hasLengthBetween
import com.drinkit.utils.hasMinLength
import com.drinkit.utils.isBetween
import com.drinkit.utils.isEmail
import java.time.Clock
import java.time.LocalDate

sealed class User(
    open val id: UserId,
    open val email: Email,
)

data class UserId(
    override val value: String,
) : AbstractId(value) {
    init {
        require(value.isId())
    }

    companion object {
        fun create(generator: IdGenerator) = UserId(value = generator.createNewId())
    }
}

data class FirstName(
    val value: String,
) {
    init {
        require(
            value.isNotBlank() &&
                value.doesntContainsInvisibleCharacters() &&
                value.hasLengthBetween(MIN_FIRSTNAME_LENGTH, MAX_FIRSTNAME_LENGTH)
        ) {
            "Invalid FirstName format (should not be blank, should not contains invisible characters, " +
                "should have size between $MIN_FIRSTNAME_LENGTH and $MAX_FIRSTNAME_LENGTH. Given value: $value"
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
                value.hasLengthBetween(MIN_LASTNAME_LENGTH, MAX_LASTNAME_LENGTH)
        ) {
            "Invalid LastName format (should not be blank, should not contains invisible characters, " +
                "should have size between $MIN_LASTNAME_LENGTH and $MAX_LASTNAME_LENGTH. Given value: $value"
        }
    }
}

data class Email(
    val value: String,
) {
    init {
        require(
            value.isNotBlank() &&
                value.isEmail() &&
                value.length <= MAX_EMAIL_LENGTH
        ) {
            "Email should be valid and have less than $MAX_EMAIL_LENGTH characters. Given value: $value"
        }
    }
}

class EncodedPassword private constructor(
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

data class BirthDate(
    val value: LocalDate,
) {
    init {
        require(value.isBetween(MIN_BIRTH_DATE, LocalDate.now(Clock.systemUTC()))) {
            "Birthdate should be between $MIN_BIRTH_DATE and now. Given value: $value"
        }
    }
}

data class Roles(
    val values: Set<Role>
) {
    enum class Role {
        ROLE_USER, ROLE_ADMIN
    }

    init {
        require(values.isNotEmpty()) {
            "A role is required here"
        }
    }

    fun allAsString() = values.map { it.name }.toSet()
}
