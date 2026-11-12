package com.drinkit.kotlin

import io.kotest.inspectors.forAll
import io.kotest.matchers.concurrent.shouldCompleteWithin
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class StringUtilsTest {

    @Test
    fun `every ASCII punctuation or symbol is a special character`() {
        // When & Then
        "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toList().forAll {
            "$it".containsASpecialCharacter() shouldBe true
        }
    }

    @Test
    fun `letters, digits and spaces are not special characters`() {
        // When & Then
        "Éléonore Château 1982".containsASpecialCharacter() shouldBe false
    }

    @Test
    fun `control and format characters are invisible`() {
        // When & Then
        listOf("\u0000", "\n", "\u007F", "\u0085", "\u200B", "\uFEFF", "\u202E").forAll {
            "Cave${it}Nord".containsNoInvisibleCharacter() shouldBe false
        }
    }

    @Test
    fun `text with accents, no-break spaces and joined emojis has no invisible character`() {
        // When & Then
        listOf("Château d'Yquem, cave\u00A0: rouges", "Cave \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67").forAll {
            it.containsNoInvisibleCharacter() shouldBe true
        }
    }

    @Test
    fun `hashes in the lower-case hexadecimal of SHA-256, so that the hashes already stored still match`() {
        // When & Then
        "abc".sha256() shouldBe "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }

    @Test
    fun `an address with a dotted local part and a subdomain is an email`() {
        // When & Then
        "jane.doe+cellar@mail.example.co.uk".isEmail() shouldBe true
    }

    @Test
    fun `an address missing a part or holding a stray character is not an email`() {
        // When & Then
        listOf(
            "jane.doe.example.com",
            "jane@example",
            "jane@example.c",
            "jane..doe@example.com",
            "jane@.example.com",
            "jane doe@example.com",
            "jane@example.com and more",
        ).forAll { it.isEmail() shouldBe false }
    }

    @Test
    fun `looks for an email in a long text in linear time`() {
        // Given
        val text = "a".repeat(40_000) + " and " + "a.".repeat(20_000)

        // When & Then
        shouldCompleteWithin(1, TimeUnit.SECONDS) { VALID_EMAIL_REGEX.containsMatchIn(text) shouldBe false }
    }

    @Test
    fun `a length on either bound is between them`() {
        // When & Then
        listOf("ab", "abcd").forAll { it.hasLengthBetween(2, 4) shouldBe true }
    }

    @Test
    fun `a length past a bound is not between them`() {
        // When & Then
        listOf("a", "abcde").forAll { it.hasLengthBetween(2, 4) shouldBe false }
    }

    @Test
    fun `camel case and Pascal case become kebab case`() {
        // When & Then
        listOf("maxCellarRoomLength", "MaxCellarRoomLength").forAll {
            it.toKebabCase() shouldBe "max-cellar-room-length"
        }
    }
}
