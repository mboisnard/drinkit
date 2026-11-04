package com.drinkit.kotlin

import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class StringUtilsTest {

    @Test
    fun `every ASCII punctuation or symbol is a special character`() {
        "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toList().forAll {
            "$it".containsASpecialCharacter() shouldBe true
        }
    }

    @Test
    fun `letters, digits and spaces are not special characters`() {
        "Éléonore Château 1982".containsASpecialCharacter() shouldBe false
    }

    @Test
    fun `control and format characters are invisible`() {
        listOf("\u0000", "\n", "\u007F", "\u0085", "\u200B", "\uFEFF", "\u202E").forAll {
            "Cave${it}Nord".containsNoInvisibleCharacter() shouldBe false
        }
    }

    @Test
    fun `text with accents, no-break spaces and joined emojis has no invisible character`() {
        listOf("Château d'Yquem, cave\u00A0: rouges", "Cave \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67").forAll {
            it.containsNoInvisibleCharacter() shouldBe true
        }
    }

    @Test
    fun `an address with a dotted local part and a subdomain is an email`() {
        "jane.doe+cellar@mail.example.co.uk".isEmail() shouldBe true
    }

    @Test
    fun `an address missing a part or holding a stray character is not an email`() {
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
    fun `camel case and Pascal case become kebab case`() {
        listOf("maxCellarRoomLength", "MaxCellarRoomLength").forAll {
            it.toKebabCase() shouldBe "max-cellar-room-length"
        }
    }
}
