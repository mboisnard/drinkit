package com.drinkit.kotlin

import com.drinkit.documentation.tech.starter.TechStarterTool

// The zero-width joiners stay: emojis such as a family and several scripts need them
val INVISIBLE_CHARS_REGEX = Regex("[\\p{Cc}\\p{Cf}&&[^\\u200C\\u200D]]")
val SPECIAL_CHAR_REGEX = Regex("[\\p{P}\\p{S}]")

// OWASP Email Regex: https://owasp.org/www-community/OWASP_Validation_Regex_Repository
val VALID_EMAIL_REGEX = Regex("[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}")

@TechStarterTool
fun String.hasMinLength(min: Int): Boolean = this.length >= min

@TechStarterTool
fun String.hasLengthBetween(min: Int, max: Int): Boolean = this.length in min..max

@TechStarterTool
fun String.containsNoInvisibleCharacter(): Boolean = !INVISIBLE_CHARS_REGEX.containsMatchIn(this)

@TechStarterTool
fun String.containsACapitalLetter(): Boolean = this.any { it.isUpperCase() }

@TechStarterTool
fun String.containsANumber(): Boolean = this.any { it.isDigit() }

@TechStarterTool
fun String.containsASpecialCharacter(): Boolean = SPECIAL_CHAR_REGEX.containsMatchIn(this)

@TechStarterTool
fun String.isEmail(): Boolean = VALID_EMAIL_REGEX.matches(this)

@TechStarterTool
fun String.toKebabCase(): String = this.replace(Regex("(?<=[a-z])(?=[A-Z])"), "-").lowercase()
