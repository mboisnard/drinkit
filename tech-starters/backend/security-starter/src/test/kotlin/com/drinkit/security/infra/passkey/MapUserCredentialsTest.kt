package com.drinkit.security.infra.passkey

import org.springframework.security.web.webauthn.management.MapUserCredentialRepository

// The double that the tests of the security chain run on
internal class MapUserCredentialsTest : UserCredentialsTestContract() {

    override val credentials = MapUserCredentialRepository()
}
