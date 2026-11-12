package com.drinkit.kotlin

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.security.MessageDigest
import java.util.HexFormat

/** The zero-width joiners stay: emojis such as a family and several scripts need them */
@TechStarterTool
val INVISIBLE_CHARS_REGEX = Regex("[\\p{Cc}\\p{Cf}&&[^\\u200C\\u200D]]")

@TechStarterTool
val SPECIAL_CHAR_REGEX = Regex("[\\p{P}\\p{S}]")

/**
 * OWASP Email Regex: https://owasp.org/www-community/OWASP_Validation_Regex_Repository. Searched in a long text, it
 * starts only where a local part starts, after an ellipsis too, and never backtracks into it: it runs in linear time.
 */
@TechStarterTool
val VALID_EMAIL_REGEX = Regex(
    "(?<![a-zA-Z0-9_+&*-])(?<![a-zA-Z0-9_+&*-]\\.)" +
        "[a-zA-Z0-9_+&*-]++(?:\\.[a-zA-Z0-9_+&*-]++)*+@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}",
)

/** A French phone number, or an international one with its country code, as written in a text. */
@TechStarterTool
val PHONE_NUMBER_REGEX = Regex("""(?:\+\d{1,3}[\s.-]?\(?\d{1,4}\)?|\b0\d)(?:[\s.-]?\d{2}){4}\b""")

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

/** In lower-case hexadecimal, the form of the hashes already stored. */
@TechStarterTool
fun String.sha256(): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(toByteArray()))
