package com.drinkit.security.infra.code

import com.drinkit.security.Browser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.TestAccounts
import com.drinkit.security.keptXsrfCookie
import com.drinkit.security.wrongCode
import com.drinkit.test.ControlledClock
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import java.time.Duration
import java.util.UUID

private const val MAX_ATTEMPTS = 3
private const val REQUESTS_PER_EMAIL_AND_HOUR = 5
private const val REQUESTS_PER_IP = 50
private const val TRIES_PER_IP = 50

@SecuredApplicationTest
internal class SignInCodeTest(
    private val mockMvc: MockMvc,
    private val accounts: InMemoryUserAccounts,
    private val codes: SpySignInCodeSender,
    private val clock: ControlledClock,
) {

    private val browser = Browser(mockMvc, codes)
    private val testAccounts = TestAccounts(accounts)

    @AfterEach
    fun resetClock() {
        clock.reset()
    }

    @Test
    fun `sends a code of six digits to the email asked for`() {
        // Given
        val account = testAccounts.user()

        // When
        browser.requestCode(account.login).andExpect { status { isNoContent() } }

        // Then
        codes.lastCodeOf(account.login) shouldMatch Regex("\\d{6}")
    }

    @Test
    fun `answers a code request for a known and for an unknown email the same way`() {
        // Given
        val account = testAccounts.user()

        // When
        val known = browser.requestCode(account.login).andReturn().response
        val unknown = browser.requestCode(anUnknownEmail()).andReturn().response

        // Then
        unknown.status shouldBe known.status
        unknown.contentAsString shouldBe known.contentAsString
        unknown.cookies.map { it.name } shouldBe known.cookies.map { it.name }
    }

    @Test
    fun `answers a code request even when the code cannot be sent`() {
        // Given
        val login = "unreachable@drinkit.test"
        codes.failFor(login)

        // When
        val request = browser.requestCode(login)

        // Then
        request.andExpect { status { isNoContent() } }
    }

    @Test
    fun `signs in with the emailed code, in a new session with a new XSRF token`() {
        // Given
        val account = testAccounts.user()
        val anonymousSessionId = browser.session.id
        val anonymousXsrf = browser.fetchXsrfCookie()
        browser.requestCode(account.login, anonymousXsrf)

        // When
        val signIn = browser.signInWithCode(account.login, codes.lastCodeOf(account.login), anonymousXsrf)
            .andExpect { status { isNoContent() } }

        // Then
        browser.session.id shouldNotBe anonymousSessionId
        val xsrf = signIn.andReturn().response.keptXsrfCookie().shouldNotBeNull()
        xsrf.value shouldNotBeIn listOf("", anonymousXsrf.value)
        browser.get("/api/cellars/me").andExpect { jsonPath("$.id") { value(account.id) } }
        browser.post("/api/cellars", xsrf).andExpect { status { isCreated() } }
    }

    @Test
    fun `finds the account whatever the case and the spaces of the email`() {
        // Given
        val account = testAccounts.user(login = "ada@drinkit.test")
        browser.requestCode("  Ada@Drinkit.test ")

        // When
        val signIn = browser.signInWithCode("ADA@drinkit.test", codes.lastCodeOf("ada@drinkit.test"))

        // Then
        signIn.andExpect { status { isNoContent() } }
        browser.get("/api/cellars/me").andExpect { jsonPath("$.id") { value(account.id) } }
    }

    @Test
    fun `signs in once with a code`() {
        // Given
        val account = testAccounts.user()
        browser.signIn(account.login).andExpect { status { isNoContent() } }

        // When
        val again = Browser(mockMvc, codes).signInWithCode(account.login, codes.lastCodeOf(account.login))

        // Then
        again.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `signs in with the right code after two wrong ones`() {
        // Given
        val account = testAccounts.user()
        browser.requestCode(account.login)
        val code = codes.lastCodeOf(account.login)
        repeat(MAX_ATTEMPTS - 1) { browser.signInWithCode(account.login, code.wrongCode()) }

        // When
        val signIn = browser.signInWithCode(account.login, code)

        // Then
        signIn.andExpect { status { isNoContent() } }
    }

    @Test
    fun `refuses the right code after three wrong ones`() {
        // Given
        val account = testAccounts.user()
        browser.requestCode(account.login)
        val code = codes.lastCodeOf(account.login)
        repeat(MAX_ATTEMPTS) { browser.signInWithCode(account.login, code.wrongCode()) }

        // When
        val signIn = browser.signInWithCode(account.login, code)

        // Then
        signIn.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `refuses a code request for what is not an email`() {
        // When
        val request = browser.post("/api/auth/code") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "not an email"}"""
        }

        // Then
        request.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `refuses a code request for an email longer than the application takes`() {
        // When
        val request = browser.post("/api/auth/code") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "${"a".repeat(90)}@drinkit.test"}"""
        }

        // Then
        request.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `gives the same 401 problem for a wrong code, an unknown email and a malformed code`() {
        // Given
        val account = testAccounts.user()
        browser.requestCode(account.login)

        // When
        val wrongCode = browser.signInWithCode(account.login, codes.lastCodeOf(account.login).wrongCode())
            .andExpect { status { isUnauthorized() } }.andReturn().response.contentAsString
        val unknownEmail = browser.signInWithCode(anUnknownEmail(), "123456")
            .andExpect { status { isUnauthorized() } }.andReturn().response.contentAsString
        val malformedCode = browser.signInWithCode(account.login, "not a code")
            .andExpect { status { isUnauthorized() } }.andReturn().response.contentAsString

        // Then
        unknownEmail shouldBe wrongCode
        malformedCode shouldBe wrongCode
    }

    @Test
    fun `creates the account of an unknown email at its first code, then signs it in`() {
        // Given
        val login = anUnknownEmail()

        // When
        browser.signIn(login).andExpect { status { isNoContent() } }

        // Then
        val account = accounts.findByLogin(login).shouldNotBeNull()
        browser.get("/api/public/me").andExpect {
            jsonPath("$.id") { value(account.id) }
            jsonPath("$.roles[0]") { value("ROLE_REGISTRATION_IN_PROGRESS") }
        }
    }

    @Test
    fun `creates no account before the code is entered`() {
        // Given
        val login = anUnknownEmail()

        // When
        browser.requestCode(login)

        // Then
        accounts.findByLogin(login) shouldBe null
    }

    @Test
    fun `refuses the sign in when the application refuses the account`() {
        // Given
        val login = anUnknownEmail()
        accounts.refuseRegistrationOf(login)

        // When
        val signIn = browser.signIn(login)

        // Then
        signIn.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `logs a sign in by code with the user id only`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()

        // When
        browser.signIn(account.login)

        // Then
        output.out shouldContain "authn_login_success:${account.id},method=email_code"
        output.out shouldNotContain account.login
    }

    @Test
    fun `logs a refused code without the email nor the code`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()
        browser.requestCode(account.login)
        val wrong = codes.lastCodeOf(account.login).wrongCode()

        // When
        browser.signInWithCode(account.login, wrong)

        // Then
        output.out shouldContain "authn_login_fail"
        output.out shouldNotContain account.login
        output.out shouldNotContain wrong
    }

    @Test
    fun `logs a malformed sign-in as a refused one`(output: CapturedOutput) {
        // When
        browser.signInWithCode("ada@drinkit.test", "not a code")

        // Then
        output.out shouldContain "authn_login_fail"
    }

    @Test
    fun `refuses a sixth code request for an email within an hour, with the wait in Retry-After`() {
        // Given
        clock.fix()
        val account = testAccounts.user()
        repeat(REQUESTS_PER_EMAIL_AND_HOUR) {
            browser.requestCode(account.login).andExpect { status { isNoContent() } }
        }

        // When
        val sixth = browser.requestCode(account.login)

        // Then
        sixth.andExpect {
            status { isTooManyRequests() }
            header { string(RETRY_AFTER, "720") }
        }
    }

    @Test
    fun `sends no email past the limit of an email, so that its live code still signs in`() {
        // Given
        val account = testAccounts.user()
        repeat(REQUESTS_PER_EMAIL_AND_HOUR) { browser.requestCode(account.login) }
        val liveCode = codes.lastCodeOf(account.login)
        browser.requestCode(account.login).andExpect { status { isTooManyRequests() } }

        // When
        val signIn = browser.signInWithCode(account.login, liveCode)

        // Then
        signIn.andExpect { status { isNoContent() } }
    }

    @Test
    fun `refuses the code requests of an IP address past its limit, whatever the email`() {
        // Given
        repeat(REQUESTS_PER_IP) { browser.requestCode(anUnknownEmail()).andExpect { status { isNoContent() } } }

        // When
        val next = browser.requestCode(anUnknownEmail())

        // Then
        next.andExpect {
            status { isTooManyRequests() }
            header { exists(RETRY_AFTER) }
        }
    }

    @Test
    fun `counts a refused IP address against none of the emails it asks for`() {
        // Given
        repeat(REQUESTS_PER_IP) { browser.requestCode(anUnknownEmail()) }
        val account = testAccounts.user()
        browser.requestCode(account.login).andExpect { status { isTooManyRequests() } }

        // When
        val fromAnotherIp = Browser(mockMvc, codes).requestCode(account.login)

        // Then
        fromAnotherIp.andExpect { status { isNoContent() } }
    }

    @Test
    fun `refuses even a right code from an IP address past its limit of tries`() {
        // Given
        repeat(TRIES_PER_IP) {
            browser.signInWithCode(anUnknownEmail(), "123456").andExpect { status { isUnauthorized() } }
        }
        val account = testAccounts.user()
        Browser(mockMvc, codes).requestCode(account.login)

        // When
        val rightCode = browser.signInWithCode(account.login, codes.lastCodeOf(account.login))

        // Then
        rightCode.andExpect {
            status { isTooManyRequests() }
            header { exists(RETRY_AFTER) }
        }
    }

    @Test
    fun `answers a limited code request the same for a known and for an unknown email`() {
        // Given
        clock.fix()
        val known = testAccounts.user().login
        val unknown = anUnknownEmail()
        repeat(REQUESTS_PER_EMAIL_AND_HOUR) {
            browser.requestCode(known)
            browser.requestCode(unknown)
        }

        // When
        val forKnown = browser.requestCode(known).andReturn().response
        val forUnknown = browser.requestCode(unknown).andReturn().response

        // Then
        forKnown.status shouldBe HttpStatus.TOO_MANY_REQUESTS.value()
        forUnknown.status shouldBe forKnown.status
        forUnknown.contentAsString shouldBe forKnown.contentAsString
        forUnknown.getHeader(RETRY_AFTER) shouldBe forKnown.getHeader(RETRY_AFTER)
    }

    @Test
    fun `signs in with a new code once the wait of a limited request has passed`() {
        // Given
        clock.fix()
        val account = testAccounts.user()
        repeat(REQUESTS_PER_EMAIL_AND_HOUR) { browser.requestCode(account.login) }
        val limited = browser.requestCode(account.login).andReturn().response
        clock.add(Duration.ofSeconds(requireNotNull(limited.getHeader(RETRY_AFTER)).toLong()))

        // When
        val signIn = browser.signIn(account.login)

        // Then
        signIn.andExpect { status { isNoContent() } }
    }

    private fun anUnknownEmail() = "${UUID.randomUUID()}@drinkit.test"
}
