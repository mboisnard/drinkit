package com.drinkit.security.infra.passkey

import com.drinkit.security.CurrentUser
import com.drinkit.security.Passkey
import com.drinkit.security.Passkeys
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository

/** Spring Security's passkeys of the signed-in user, whose user entity is named after the account id. */
internal class SpringPasskeys(
    private val userEntities: PublicKeyCredentialUserEntityRepository,
    private val userCredentials: UserCredentialRepository,
    private val currentUser: CurrentUser,
) : Passkeys {

    override fun ofCurrentUser(): List<Passkey> = userEntities.findByUsername(currentUser.get().id)
        ?.let { userEntity -> userCredentials.findByUserId(userEntity.id) }
        .orEmpty()
        .map { passkey ->
            Passkey(
                passkey.credentialId.toBase64UrlString(),
                passkey.label,
                passkey.created,
                passkey.lastUsed,
            )
        }
}
