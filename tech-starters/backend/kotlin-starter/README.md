# Kotlin Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
of plain Kotlin extension functions that serve the values of the domain and their arithmetic, such as the checks of
a password or an email address, and a division at a given scale. It has no Spring bean and no configuration.

## Key Features

- `containsNoInvisibleCharacter` refuses any Unicode control or format character (categories `Cc` and `Cf`): a line
  break, a tab, DEL, a zero-width space, a byte order mark, a bidirectional mark. A no-break space is accepted,
  since French text puts one before a colon, and so are the zero-width joiners that an emoji such as a family needs.
- `containsASpecialCharacter` finds any Unicode punctuation or symbol (categories `P` and `S`). That covers every
  printable ASCII character that is neither a letter, a digit nor a space, and others such as `€` or `«`.
- `isEmail` matches the whole string against the
  [OWASP email pattern](https://owasp.org/www-community/OWASP_Validation_Regex_Repository), which accepts ASCII
  addresses only.
- `hasLengthBetween` and `BigDecimal.isBetween` include their bounds. `LocalDate.isStrictlyBetween` excludes them.
- `safelyDivide` stops at the scale you give and rounds half to even, the `SAFE_ROUNDING_MODE`. A quotient with
  no end, such as one divided by three, never throws.
- `addUnless(condition, value)` adds the value to a list when the condition is false. Inside `buildList`, it
  collects every rule a value breaks.

The starter also exposes kotlin-logging, kotlin-reflect and kotlinx-coroutines to the modules that depend on it.

## Usage

The `library-convention` and `api-convention` plugins of `build-logic` already add the starter. Any other module
declares it:

```kotlin
dependencies {
    implementation(project(":kotlin-starter"))
}
```

A value of the domain lists the rules it breaks:

```kotlin
data class CellarName(val value: String) {
    fun validate() = buildList {
        addUnless(value.containsNoInvisibleCharacter(), "Cellar name holds an invisible character, $value")
        addUnless(value.hasLengthBetween(1, MAX_CELLAR_NAME_LENGTH), "Cellar name is too long, $value")
    }
}
```
