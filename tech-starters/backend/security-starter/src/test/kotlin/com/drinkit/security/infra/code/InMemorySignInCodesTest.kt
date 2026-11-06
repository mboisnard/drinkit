package com.drinkit.security.infra.code

internal class InMemorySignInCodesTest : SignInCodesTestContract() {
    override val codes: SignInCodes = InMemorySignInCodes()
}
