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
        roundTrip("""{"currency": "EUR"}""") shouldBe """{"currency": "EUR"}"""
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
        read("""["java.io.File", "/etc/passwd"]""".toByteArray()).shouldBeNull()
    }

    private fun roundTrip(value: Any) = read(
        converter.convert(value, TypeDescriptor.valueOf(Any::class.java), TypeDescriptor.valueOf(ByteArray::class.java))
            as ByteArray,
    )

    private fun read(bytes: ByteArray) =
        converter.convert(bytes, TypeDescriptor.valueOf(ByteArray::class.java), TypeDescriptor.valueOf(Any::class.java))
}
