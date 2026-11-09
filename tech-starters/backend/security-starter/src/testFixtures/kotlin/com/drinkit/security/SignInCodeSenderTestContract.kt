package com.drinkit.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

abstract class SignInCodeSenderTestContract {

    private val sender: SignInCodeSender by lazy { fetchSignInCodeSender() }

    abstract fun fetchSignInCodeSender(): SignInCodeSender

    /** The last code on its way to [login], null when none is. */
    abstract fun codeSentTo(login: String): String?

    @Test
    fun `sends the code to the login it names`() {
        // When
        sender.send("ada@drinkit.test", "042137", Locale.FRENCH)

        // Then
        codeSentTo("ada@drinkit.test") shouldBe "042137"
    }

    @Test
    fun `sends no code to another login`() {
        // When
        sender.send("ada@drinkit.test", "042137", Locale.FRENCH)

        // Then
        codeSentTo("grace@drinkit.test") shouldBe null
    }
}
