package com.drinkit.security.infra.session

import org.springframework.security.web.webauthn.api.AttestationConveyancePreference
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInput
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement
import org.springframework.security.web.webauthn.api.UserVerificationRequirement
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.node.ObjectNode
import java.time.Duration

private val PARAMETERS = listOf(
    PublicKeyCredentialParameters.EdDSA,
    PublicKeyCredentialParameters.ES256,
    PublicKeyCredentialParameters.ES384,
    PublicKeyCredentialParameters.ES512,
    PublicKeyCredentialParameters.RS256,
    PublicKeyCredentialParameters.RS384,
    PublicKeyCredentialParameters.RS512,
    PublicKeyCredentialParameters.RS1,
)

private val USER_VERIFICATIONS = listOf(
    UserVerificationRequirement.DISCOURAGED,
    UserVerificationRequirement.PREFERRED,
    UserVerificationRequirement.REQUIRED,
)

/**
 * The options of a passkey ceremony, which Spring Security keeps in the session between the options and the answer
 * of the browser, as the JSON of the WebAuthn specification. Spring writes that JSON for the browser but reads none of
 * it back, since it expects a session in Java serialization: this reads it back.
 *
 * W3C WebAuthn Level 3, 5.4 and 5.5 (the options) and 5.1.8 and 5.1.9 (their JSON form).
 */
internal object PasskeyOptionsJson {

    private val webAuthnJson = JsonMapper.builder().addModule(WebauthnJacksonModule()).build()

    val module: SimpleModule = SimpleModule()
        .addDeserializer(PublicKeyCredentialCreationOptions::class.java, Reader(::creationOptions))
        .addDeserializer(PublicKeyCredentialRequestOptions::class.java, Reader(::requestOptions))

    fun isOptions(value: Any) =
        value is PublicKeyCredentialCreationOptions || value is PublicKeyCredentialRequestOptions

    /** The JSON of the specification, behind the `@class` of the session's other attributes. */
    fun write(options: Any): ByteArray {
        val json = webAuthnJson.createObjectNode()
            .put("@class", options.javaClass.name)
            .setAll(webAuthnJson.valueToTree<ObjectNode>(options))

        return webAuthnJson.writeValueAsBytes(json)
    }

    fun allowIn(types: BasicPolymorphicTypeValidator.Builder): BasicPolymorphicTypeValidator.Builder = types
        .allowIfSubType(PublicKeyCredentialCreationOptions::class.java)
        .allowIfSubType(PublicKeyCredentialRequestOptions::class.java)

    private class Reader<T : Any>(private val read: (JsonNode) -> T) : ValueDeserializer<T>() {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): T =
            read(parser.readValueAsTree())
    }
}

private fun creationOptions(json: JsonNode): PublicKeyCredentialCreationOptions =
    PublicKeyCredentialCreationOptions.builder()
        .rp(PublicKeyCredentialRpEntity.builder().id(json["rp"].text("id")).name(json["rp"].text("name")).build())
        .user(
            ImmutablePublicKeyCredentialUserEntity.builder()
                .id(json["user"].bytes("id"))
                .name(json["user"]["name"].asString())
                .displayName(json["user"].text("displayName"))
                .build(),
        )
        .challenge(json.bytes("challenge"))
        .pubKeyCredParams(json["pubKeyCredParams"].values().map { it.parameters() })
        .timeout(json.timeout())
        .excludeCredentials(json.descriptors("excludeCredentials"))
        .authenticatorSelection(json["authenticatorSelection"].authenticatorSelection())
        .attestation(json.text("attestation")?.let(AttestationConveyancePreference::valueOf))
        .extensions(json.extensions())
        .build()

private fun requestOptions(json: JsonNode): PublicKeyCredentialRequestOptions =
    PublicKeyCredentialRequestOptions.builder()
        .challenge(json.bytes("challenge"))
        .timeout(json.timeout())
        .rpId(json.text("rpId"))
        .allowCredentials(json.descriptors("allowCredentials"))
        .userVerification(json.userVerification())
        .extensions(json.extensions())
        .build()

private fun JsonNode.parameters() = PARAMETERS.first { it.alg.value == this["alg"].asLong() }

private fun JsonNode.authenticatorSelection() = AuthenticatorSelectionCriteria.builder()
    .authenticatorAttachment(text("authenticatorAttachment")?.let(AuthenticatorAttachment::valueOf))
    .residentKey(text("residentKey")?.let(ResidentKeyRequirement::valueOf))
    .userVerification(userVerification())
    .build()

private fun JsonNode.descriptors(field: String) = path(field).values().map { descriptor ->
    PublicKeyCredentialDescriptor.builder()
        .type(PublicKeyCredentialType.valueOf(descriptor["type"].asString()))
        .id(descriptor.bytes("id"))
        .transports(descriptor.transports())
        .build()
}

private fun JsonNode.transports(): Set<AuthenticatorTransport>? = this["transports"]
    ?.takeUnless { it.isNull }
    ?.values()
    ?.map { AuthenticatorTransport.valueOf(it.asString()) }
    ?.toSet()

// Each extension Spring Security asks for, such as credProps, takes a boolean
private fun JsonNode.extensions() = ImmutableAuthenticationExtensionsClientInputs(
    path("extensions").properties().map { (name, value) ->
        ImmutableAuthenticationExtensionsClientInput(name, value.asBoolean())
    },
)

private fun JsonNode.userVerification() =
    text("userVerification")?.let { value -> USER_VERIFICATIONS.first { it.value == value } }

// The specification writes the timeout in milliseconds
private fun JsonNode.timeout() = Duration.ofMillis(this["timeout"].asLong())

private fun JsonNode.bytes(field: String) = Bytes.fromBase64(text(field))

private fun JsonNode.text(field: String): String? = this[field]?.takeUnless { it.isNull }?.asString()
