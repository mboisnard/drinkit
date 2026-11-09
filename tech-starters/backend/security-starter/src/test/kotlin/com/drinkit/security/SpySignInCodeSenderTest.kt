package com.drinkit.security

internal class SpySignInCodeSenderTest : SignInCodeSenderTestContract() {

    private val spy = SpySignInCodeSender()

    override fun fetchSignInCodeSender(): SignInCodeSender = spy

    override fun codeSentTo(login: String) = if (spy.hasSentTo(login)) spy.lastCodeOf(login) else null
}
