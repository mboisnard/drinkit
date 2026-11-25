package com.drinkit.security.infra.passkey

import org.springframework.security.web.webauthn.management.MapPublicKeyCredentialUserEntityRepository

// The double that the tests of the security chain run on
internal class MapUserEntitiesTest : UserEntitiesTestContract() {

    override val userEntities = MapPublicKeyCredentialUserEntityRepository()
}
