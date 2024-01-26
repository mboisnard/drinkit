package com.drinkit.user

import com.drinkit.common.*
import com.drinkit.common.Constants.MAX_EMAIL_LENGTH
import com.drinkit.common.Constants.MAX_FIRSTNAME_LENGTH
import com.drinkit.common.Constants.MAX_LASTNAME_LENGTH
import com.drinkit.common.Constants.MIN_BIRTH_DATE
import com.drinkit.common.Constants.MIN_FIRSTNAME_LENGTH
import com.drinkit.common.Constants.MIN_LASTNAME_LENGTH
import com.drinkit.common.Constants.MIN_PASSWORD_LENGTH
import com.drinkit.common.Constants.VALID_EMAIL_REGEX
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class UserId(
    override val value: String,
): AbstractId(value) {
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
        require(value.isNotBlank()
                && !value.containsInvisibleCharacters()
                && value.hasLengthBetween(MIN_FIRSTNAME_LENGTH, MAX_FIRSTNAME_LENGTH)) {
            "Invalid FirstName format (should not be blank, should not contains invisible characters, should have size between $MIN_FIRSTNAME_LENGTH and $MAX_FIRSTNAME_LENGTH"
        }
    }
}

data class LastName(
    val value: String,
) {
    init {
        require(value.isNotBlank()
                && !value.containsInvisibleCharacters()
                && value.hasLengthBetween(MIN_LASTNAME_LENGTH, MAX_LASTNAME_LENGTH)) {
            "Invalid LastName format (should not be blank, should not contains invisible characters, should have size between $MIN_LASTNAME_LENGTH and $MAX_LASTNAME_LENGTH"
        }
    }
}

data class Email(
    val value: String,
) {
    init {
        require(value.isNotBlank() && VALID_EMAIL_REGEX.matches(value) && value.length <= MAX_EMAIL_LENGTH) {
            "Email should be valid and have less than $MAX_EMAIL_LENGTH characters"
        }
    }
}

data class EncodedPassword(
    val value: String,
) {
    init {
        require(value.isNotBlank() && !value.containsInvisibleCharacters())
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
        require(value.isNotBlank()
                && value.hasMinLength(MIN_PASSWORD_LENGTH)
                && !value.containsInvisibleCharacters()
                && value.containsACapitalLetter()
                && value.containsANumber()
                && value.containsSpecialCharacter()
        )
    }
}

data class BirthDate(
    val value: LocalDate,
) {
    init {
        require(value.isBetween(MIN_BIRTH_DATE, LocalDate.now(Clock.systemUTC())))
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
}

data class User(
    val id: UserId,
    val firstname: FirstName,
    val lastName: LastName,
    val birthDate: BirthDate?,
    val email: Email,

    val lastConnection: LocalDateTime?,
    val roles: Roles,
    val enabled: Boolean,
) {
    val isAdmin = roles.values.contains(Roles.Role.ROLE_ADMIN)
}