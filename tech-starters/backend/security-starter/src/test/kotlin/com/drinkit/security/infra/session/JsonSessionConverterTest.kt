package com.drinkit.security.infra.session

import com.drinkit.security.infra.events.SecurityEventLogger
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.core.convert.TypeDescriptor
import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.FactorGrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference
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
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement
import org.springframework.security.web.webauthn.api.UserVerificationRequirement
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant

@ExtendWith(OutputCaptureExtension::class)
internal class JsonSessionConverterTest {

    private val converter = JsonSessionConverter(javaClass.classLoader, SecurityEventLogger())

    private val user = User.withUsername("user-1")
        .password("")
        .authorities(SimpleGrantedAuthority("ROLE_USER"))
        .build()

    @Test
    fun `reads back a sign-in by code with its factor`() {
        // Given
        val factor = FactorGrantedAuthority.withAuthority("FACTOR_OTT").issuedAt(Instant.EPOCH).build()
        val login = UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities + factor)

        // When
        val read = roundTrip(SecurityContextImpl(login))

        // Then
        read shouldBe SecurityContextImpl(login)
    }

    @Test
    fun `reads back a remembered login`() {
        // Given
        val login = RememberMeAuthenticationToken("key", user, user.authorities)

        // When
        val read = roundTrip(SecurityContextImpl(login))

        // Then
        read shouldBe SecurityContextImpl(login)
    }

    @Test
    fun `reads back a login whose authorities came from Kotlin collections`() {
        // Given
        val withOne = UsernamePasswordAuthenticationToken.authenticated(user, null, setOf(SimpleGrantedAuthority("A")))
        val withNone = UsernamePasswordAuthenticationToken.authenticated(user, null, emptySet())

        // When
        val read = listOf(withOne, withNone).map { roundTrip(SecurityContextImpl(it)) }

        // Then
        read shouldBe listOf(SecurityContextImpl(withOne), SecurityContextImpl(withNone))
    }

    @Test
    fun `reads back a text`() {
        // When
        val read = roundTrip("""{"currency": "EUR"}""")

        // Then
        read shouldBe """{"currency": "EUR"}"""
    }

    @Test
    fun `reads an unreadable value as absent, and logs it`(output: CapturedOutput) {
        // When
        val read = read("""{"@class": "com.example.Gone"}""".toByteArray())

        // Then
        read.shouldBeNull()
        output.out shouldContain "session_deserialization_fail"
    }

    @Test
    fun `refuses to read a class outside the allowed ones`() {
        // When
        val read = read("""["java.io.File", "/etc/passwd"]""".toByteArray())

        // Then
        read.shouldBeNull()
    }

    @Test
    fun `reads back the options of a passkey creation`() {
        // Given
        val options = PublicKeyCredentialCreationOptions.builder()
            .rp(PublicKeyCredentialRpEntity.builder().id("drinkit.example").name("DrinkIt").build())
            .user(passkeyUser)
            .challenge(Bytes.random())
            .pubKeyCredParams(PublicKeyCredentialParameters.EdDSA, PublicKeyCredentialParameters.ES256)
            .timeout(Duration.ofMinutes(5))
            .excludeCredentials(
                listOf(
                    PublicKeyCredentialDescriptor.builder()
                        .id(Bytes.random())
                        .transports(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID)
                        .build(),
                ),
            )
            .authenticatorSelection(
                AuthenticatorSelectionCriteria.builder()
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .userVerification(UserVerificationRequirement.REQUIRED)
                    .build(),
            )
            .attestation(AttestationConveyancePreference.NONE)
            .extensions(
                ImmutableAuthenticationExtensionsClientInputs(ImmutableAuthenticationExtensionsClientInput.credProps),
            )
            .build()

        // When
        val read = roundTrip(options)

        // Then
        webAuthnJson(read) shouldBe webAuthnJson(options)
    }

    @Test
    fun `reads back the options of a passkey sign-in`() {
        // Given
        val options = PublicKeyCredentialRequestOptions.builder()
            .challenge(Bytes.random())
            .timeout(Duration.ofMinutes(5))
            .rpId("drinkit.example")
            .userVerification(UserVerificationRequirement.REQUIRED)
            .allowCredentials(listOf(PublicKeyCredentialDescriptor.builder().id(Bytes.random()).build()))
            .build()

        // When
        val read = roundTrip(options)

        // Then
        webAuthnJson(read) shouldBe webAuthnJson(options)
    }

    @Test
    fun `reads back a sign-in by passkey with its factor`() {
        // Given
        val factor = FactorGrantedAuthority.withAuthority("FACTOR_WEBAUTHN").issuedAt(Instant.EPOCH).build()
        val login = WebAuthnAuthentication(passkeyUser, listOf(SimpleGrantedAuthority("ROLE_USER"), factor))

        // When
        val read = roundTrip(SecurityContextImpl(login))

        // Then
        read shouldBe SecurityContextImpl(login)
    }

    private val passkeyUser = ImmutablePublicKeyCredentialUserEntity.builder()
        .id(Bytes.random())
        .name("user-1")
        .displayName("ada@drinkit.example")
        .build()

    // The browser form of the WebAuthn options, which Spring writes in its responses
    private fun webAuthnJson(options: Any?) =
        JsonMapper.builder().addModule(WebauthnJacksonModule()).build().writeValueAsString(options)

    private fun roundTrip(value: Any) = read(
        converter.convert(value, TypeDescriptor.valueOf(Any::class.java), TypeDescriptor.valueOf(ByteArray::class.java))
            as ByteArray,
    )

    private fun read(bytes: ByteArray) =
        converter.convert(bytes, TypeDescriptor.valueOf(ByteArray::class.java), TypeDescriptor.valueOf(Any::class.java))
}
