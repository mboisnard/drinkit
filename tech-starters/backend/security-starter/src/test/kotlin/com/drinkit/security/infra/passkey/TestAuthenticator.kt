package com.drinkit.security.infra.passkey

import com.drinkit.security.Browser
import com.drinkit.security.PASSKEY_ORIGIN
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialRequestOptions
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.client.Origin
import com.webauthn4j.test.authenticator.webauthn.NoneAttestationAuthenticator
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor
import com.webauthn4j.test.client.ClientPlatform
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import java.util.Base64

private val json = ObjectConverter().jsonMapper

/**
 * A platform authenticator, as in a phone or a laptop, which verifies its user and keeps its passkeys, in a browser
 * that runs the pages of [origin]. It speaks the JSON of the WebAuthn specification, as Spring's script does.
 */
internal class TestAuthenticator(origin: String = PASSKEY_ORIGIN) {

    private val authenticator = NoneAttestationAuthenticator()
    private val client = ClientPlatform(Origin(origin), WebAuthnAuthenticatorAdaptor(authenticator))
    private val passkeyIds = mutableListOf<ByteArray>()
    private var rpId: String? = null

    fun movesTo(origin: String) {
        client.origin = Origin(origin)
    }

    /** Signs for [rpId] from now on, whatever the options name, as the passkeys of another site would. */
    fun signsFor(rpId: String) {
        this.rpId = rpId
        passkeyIds.forEach { authenticator.lookup(it).rpId = rpId }
    }

    /** The body that saves a new passkey, made from the creation [options] the application gave. */
    fun create(options: String, label: String = "Ada's phone"): String {
        val credential = client.create(
            requireNotNull(json.readValue(options, PublicKeyCredentialCreationOptions::class.java)),
        )
        val response = requireNotNull(credential.response)
        passkeyIds += credential.rawId

        val publicKey = mapOf(
            "id" to credential.id,
            "rawId" to credential.rawId.base64Url(),
            "type" to credential.type,
            "response" to mapOf(
                "attestationObject" to response.attestationObject.base64Url(),
                "clientDataJSON" to response.clientDataJSON.base64Url(),
                "transports" to listOf("internal"),
            ),
            "clientExtensionResults" to emptyMap<String, Any>(),
            "authenticatorAttachment" to "platform",
        )

        return json.writeValueAsString(mapOf("publicKey" to mapOf("credential" to publicKey, "label" to label)))
    }

    /**
     * The body that signs in, made from the sign-in [options] the application gave. The user verification they ask
     * for is skipped when [verifiesUser] is false, as by a security key without PIN.
     */
    fun get(options: String, verifiesUser: Boolean = true): String {
        val asked = requireNotNull(json.readValue(options, PublicKeyCredentialRequestOptions::class.java))
        val userVerification = if (verifiesUser) asked.userVerification else UserVerificationRequirement.DISCOURAGED
        val assertion = client.get(asked.copy(rpId ?: asked.rpId, userVerification))
        val response = requireNotNull(assertion.response)

        return json.writeValueAsString(
            mapOf(
                "id" to assertion.id,
                "rawId" to assertion.rawId.base64Url(),
                "type" to assertion.type,
                "response" to mapOf(
                    "authenticatorData" to response.authenticatorData.base64Url(),
                    "clientDataJSON" to response.clientDataJSON.base64Url(),
                    "signature" to response.signature.base64Url(),
                    "userHandle" to response.userHandle?.base64Url(),
                ),
                "clientExtensionResults" to emptyMap<String, Any>(),
                "authenticatorAttachment" to "platform",
            ),
        )
    }

    private fun PublicKeyCredentialRequestOptions.copy(rpId: String?, userVerification: UserVerificationRequirement?) =
        PublicKeyCredentialRequestOptions(challenge, timeout, rpId, allowCredentials, userVerification, extensions)
}

private fun ByteArray.base64Url() = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

/** Asks for the options of a new passkey, creates it on [authenticator], and saves it with its [label]. */
internal fun Browser.registerPasskey(
    authenticator: TestAuthenticator,
    label: String = "Ada's phone",
): ResultActionsDsl = savePasskey(authenticator.create(passkeyOptions("/webauthn/register/options"), label))

/** Sends the answer of an authenticator to the creation options asked last. */
internal fun Browser.savePasskey(creation: String): ResultActionsDsl = post("/webauthn/register") {
    contentType = MediaType.APPLICATION_JSON
    content = creation
}

/** Signs in with a passkey of [authenticator], without any email, as from the autofill of the email field. */
internal fun Browser.signInWithPasskey(
    authenticator: TestAuthenticator,
    verifiesUser: Boolean = true,
): ResultActionsDsl {
    val body = authenticator.get(passkeyOptions("/webauthn/authenticate/options"), verifiesUser)

    return post("/login/webauthn") {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }
}

internal fun Browser.passkeyOptions(path: String): String =
    post(path).andExpect { status { isOk() } }.andReturn().response.contentAsString
