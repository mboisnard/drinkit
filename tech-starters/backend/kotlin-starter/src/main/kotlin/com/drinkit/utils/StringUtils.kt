package com.drinkit.utils

val INVISIBLE_CHARS_REGEX = Regex("[\\u0000-\\u001f]")
val SPECIAL_CHAR_REGEX = Regex("[!@#\$%&*()_+=|<>?{}\\\\[\\\\]~-]")

// OWASP Email Regex: https://owasp.org/www-community/OWASP_Validation_Regex_Repository
val VALID_EMAIL_REGEX = Regex("[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}")

fun String.hasMinLength(min: Int): Boolean =
    this.length >= min

fun String.hasLengthBetween(min: Int, max: Int): Boolean =
    this.length in min..max

fun String.doesntContainsInvisibleCharacters(): Boolean =
    !INVISIBLE_CHARS_REGEX.containsMatchIn(this)

fun String.containsACapitalLetter(): Boolean =
    this.any { it.isUpperCase() }

fun String.containsANumber(): Boolean =
    this.any { it.isDigit() }

fun String.containsASpecialCharacter(): Boolean =
    SPECIAL_CHAR_REGEX.containsMatchIn(this)

fun String.isEmail(): Boolean =
    VALID_EMAIL_REGEX.matches(this)
