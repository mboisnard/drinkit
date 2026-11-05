package com.drinkit.security.infra.rememberme

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.CookieBrowser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.PostgresSessionsTest
import com.drinkit.security.REMEMBER_ME_COOKIE
import com.drinkit.security.SESSION_COOKIE
import com.drinkit.security.TestAccounts
import com.drinkit.security.USER_PASSWORD
import com.drinkit.security.XSRF_COOKIE
import com.drinkit.security.generated.jooq.DrinkitSession
import com.drinkit.test.ControlledClock
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.HexFormat

// A Linux clock gives nanoseconds, which PostgreSQL rounds to microseconds: the tests run on such an instant everywhere
private val NOW_WITH_NANOSECONDS = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusNanos(999_999_600)

@JooqIntegrationTest(schemas = [DrinkitSession::class])
@PostgresSessionsTest
internal class RememberMeTest(
    private val mockMvc: MockMvc,
    private val database: JdbcClient,
    accounts: InMemoryUserAccounts,
    encoder: PasswordEncoder,
    private val clock: ControlledClock,
) {

    private val account = TestAccounts(accounts, encoder).user()
    private val browser = CookieBrowser(mockMvc)

    @AfterEach
    fun resetClock() {
        clock.reset()
    }

    @Test
    fun `remembers a login that asks for it, for the whole site and 30 days, out of reach of scripts`() {
        // Given
        val underContextPath = CookieBrowser(mockMvc, contextPath = "/drinkit")

        // When
        val login = underContextPath.login(account.login, USER_PASSWORD, rememberMe = true).andReturn().response

        // Then
        val setCookie = login.getHeaders(HttpHeaders.SET_COOKIE).single { it.startsWith("$REMEMBER_ME_COOKIE=") }
        setCookie.split(";").map { it.trim() }.drop(1) shouldContainAll
            listOf("Path=/", "Max-Age=2592000", "Secure", "HttpOnly")
        // The mock response leaves SameSite out of the header it formats, which Tomcat writes
        login.getCookie(REMEMBER_ME_COOKIE)?.getAttribute("SameSite") shouldBe "Lax"
    }

    @Test
    fun `does not remember a login that does not ask for it`() {
        // When
        val login = browser.login(account.login, USER_PASSWORD).andReturn().response

        // Then
        login.getCookie(REMEMBER_ME_COOKIE).shouldBeNull()
        seriesOf(account.id).shouldBeEmpty()
    }

    @Test
    fun `keeps only a hash of the token, and the browser it was given to`() {
        // When
        browser.login(account.login, USER_PASSWORD, rememberMe = true) { header(HttpHeaders.USER_AGENT, "Firefox") }

        // Then
        val (series, token) = decode(rememberMeCookie())
        database.sql("SELECT token_hash, user_agent FROM drinkit_session.remember_me_token WHERE series = ?")
            .param(series).query { row, _ -> row.getString(1) to row.getString(2) }
            .single() shouldBe (sha256(token) to "Firefox")
    }

    @Test
    fun `signs a remembered user in, in a new session with a rotated token and a new XSRF token`() {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val first = rememberMeCookie()
        val returning = returningBrowser().apply { keep(Cookie(XSRF_COOKIE, "token-of-the-ended-session")) }

        // When
        val visit = returning.get("/api/cellars/me")

        // Then
        visit.andExpect { jsonPath("$.id") { value(account.id) } }
        returning.cookie(SESSION_COOKIE).shouldNotBeNull()
        val rotated = decode(returning.cookie(REMEMBER_ME_COOKIE).shouldNotBeNull())
        rotated.first shouldBe decode(first).first
        rotated.second shouldNotBe decode(first).second
        returning.cookie(XSRF_COOKIE)?.value shouldNotBeIn listOf(null, "token-of-the-ended-session")
        returning.post("/api/cellars").andExpect { status { isCreated() } }
    }

    @Test
    fun `logs a remembered login as such`(output: CapturedOutput) {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)

        // When
        returningBrowser().get("/api/cellars/me")

        // Then
        output.out shouldContain "authn_login_success:${account.id},method=remember_me"
    }

    @Test
    fun `asks a remembered user for the password again before an action that needs it`() {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val returning = returningBrowser()
        returning.get("/api/cellars/me").andExpect { status { isOk() } }

        // When
        val sensitive = returning.get("/api/cellars/password-checked")

        // Then
        sensitive.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `lets a user who gave the password through an action that needs it`() {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)

        // When
        val sensitive = browser.get("/api/cellars/password-checked")

        // Then
        sensitive.andExpect { status { isNoContent() } }
    }

    @Test
    fun `accepts the previous token for a few seconds, for the requests sent at once`(output: CapturedOutput) {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val first = rememberMeCookie()
        clock.fix(NOW_WITH_NANOSECONDS)
        returningBrowser(first).get("/api/cellars/me")

        // When
        clock.add(Duration.ofSeconds(9))
        val parallel = returningBrowser(first)
        val visit = parallel.get("/api/cellars/me")

        // Then
        visit.andExpect { status { isOk() } }
        parallel.cookie(REMEMBER_ME_COOKIE)?.value shouldBe first.value
        output.out shouldContain "authn_token_grace_used:${account.id}"
    }

    @Test
    fun `signs the user out everywhere when a previous token comes back later, as a stolen one would`(
        output: CapturedOutput,
    ) {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val stolen = rememberMeCookie()
        clock.fix(NOW_WITH_NANOSECONDS)
        returningBrowser(stolen).get("/api/cellars/me")

        // When
        clock.add(Duration.ofSeconds(11))
        val replay = returningBrowser(stolen).get("/api/cellars/me")

        // Then
        replay.andExpect { status { isUnauthorized() } }
        seriesOf(account.id).shouldBeEmpty()
        sessionsOf(account.id).shouldBeEmpty()
        output.out shouldContain "authn_token_reuse:${account.id}"
    }

    @Test
    fun `forgets a login 30 days after the password was given, however often the user came back`() {
        // Given
        clock.fix(NOW_WITH_NANOSECONDS)
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val returning = returningBrowser()
        clock.add(Duration.ofDays(29))
        returning.get("/api/cellars/me").andExpect { status { isOk() } }
        returning.forget(SESSION_COOKIE)

        // When
        clock.add(Duration.ofDays(1))
        val visit = returning.get("/api/cellars/me")

        // Then
        visit.andExpect { status { isUnauthorized() } }
        returning.cookie(REMEMBER_ME_COOKIE).shouldBeNull()
        seriesOf(account.id).shouldBeEmpty()
    }

    @Test
    fun `gives a rotated token only the days left to its login`() {
        // Given
        clock.fix(NOW_WITH_NANOSECONDS)
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        clock.add(Duration.ofDays(10))

        // When
        val returning = returningBrowser()
        returning.get("/api/cellars/me")

        // Then
        returning.cookie(REMEMBER_ME_COOKIE)?.maxAge shouldBe Duration.ofDays(20).toSeconds().toInt()
    }

    @Test
    fun `purges the forgotten logins when it remembers a new one`() {
        // Given
        clock.fix(NOW_WITH_NANOSECONDS)
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        clock.add(Duration.ofDays(30).plusSeconds(1))

        // When
        val laptop = CookieBrowser(mockMvc).apply { login(account.login, USER_PASSWORD, rememberMe = true) }

        // Then
        val laptopSeries = decode(laptop.cookie(REMEMBER_ME_COOKIE).shouldNotBeNull()).first
        seriesOf(account.id) shouldContainExactly listOf(laptopSeries)
    }

    @Test
    fun `ends the series a browser presents when it logs in again with its password`() {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val previous = rememberMeCookie()
        val phone = CookieBrowser(mockMvc).apply { login(account.login, USER_PASSWORD, rememberMe = true) }

        // When
        browser.login(account.login, USER_PASSWORD, rememberMe = true)

        // Then
        seriesOf(account.id) shouldContainExactlyInAnyOrder
            listOf(decode(rememberMeCookie()).first, decode(phone.cookie(REMEMBER_ME_COOKIE).shouldNotBeNull()).first)
        returningBrowser(previous).get("/api/cellars/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `refuses an unknown series, and forgets its cookie`() {
        // Given
        val returning = CookieBrowser(mockMvc).apply {
            keep(Cookie(REMEMBER_ME_COOKIE, encode("unknown-series", "a-token")))
        }

        // When
        val visit = returning.get("/api/cellars/me")

        // Then
        visit.andExpect { status { isUnauthorized() } }
        returning.cookie(REMEMBER_ME_COOKIE).shouldBeNull()
    }

    @Test
    fun `forgets only the login of the browser that logs out`() {
        // Given
        browser.login(account.login, USER_PASSWORD, rememberMe = true)
        val phone = CookieBrowser(mockMvc).apply { login(account.login, USER_PASSWORD, rememberMe = true) }
        val phoneSeries = decode(phone.cookie(REMEMBER_ME_COOKIE).shouldNotBeNull()).first

        // When
        browser.post("/api/auth/logout").andExpect { status { isNoContent() } }

        // Then
        seriesOf(account.id) shouldContainExactly listOf(phoneSeries)
    }

    private fun rememberMeCookie() = browser.cookie(REMEMBER_ME_COOKIE).shouldNotBeNull()

    // A browser opened again after its session ended: it only has its remember-me cookie
    private fun returningBrowser(cookie: Cookie = rememberMeCookie()) = CookieBrowser(mockMvc).apply { keep(cookie) }

    private fun seriesOf(userId: String) =
        database.sql("SELECT series FROM drinkit_session.remember_me_token WHERE user_id = ?")
            .param(userId).query(String::class.java).list()

    private fun sessionsOf(userId: String) =
        database.sql("SELECT session_id FROM drinkit_session.spring_session WHERE principal_name = ?")
            .param(userId).query(String::class.java).list()

    private fun decode(cookie: Cookie): Pair<String, String> =
        String(Base64.getDecoder().decode(cookie.value.padEnd((cookie.value.length + 3) / 4 * 4, '=')))
            .split(":").let { it[0] to it[1] }

    private fun encode(series: String, token: String) =
        Base64.getEncoder().withoutPadding().encodeToString("$series:$token".toByteArray())

    private fun sha256(token: String) =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
}
