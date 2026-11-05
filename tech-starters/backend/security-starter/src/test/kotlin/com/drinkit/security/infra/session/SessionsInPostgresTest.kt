package com.drinkit.security.infra.session

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.CookieBrowser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.PostgresSessionsTest
import com.drinkit.security.REMEMBER_ME_COOKIE
import com.drinkit.security.SESSION_COOKIE
import com.drinkit.security.Sessions
import com.drinkit.security.TestAccounts
import com.drinkit.security.USER_PASSWORD
import com.drinkit.security.generated.jooq.DrinkitSession
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@JooqIntegrationTest(schemas = [DrinkitSession::class])
@PostgresSessionsTest
internal class SessionsInPostgresTest(
    private val mockMvc: MockMvc,
    private val database: JdbcClient,
    private val transactions: TransactionTemplate,
    accounts: InMemoryUserAccounts,
    encoder: PasswordEncoder,
    private val sessions: Sessions,
) {

    private val browser = CookieBrowser(mockMvc)
    private val testAccounts = TestAccounts(accounts, encoder)

    @Test
    fun `keeps a login across requests, in JSON`() {
        // Given
        val account = testAccounts.user()

        // When
        browser.login(account.login, USER_PASSWORD)

        // Then
        browser.get("/api/cellars/me").andExpect { jsonPath("$.id") { value(account.id) } }
        attributesOf(account.id).map { it["@class"] } shouldContainExactly
            listOf("org.springframework.security.core.context.SecurityContextImpl")
    }

    @Test
    fun `sets the session cookie for the whole site, out of reach of scripts`() {
        // Given
        val account = testAccounts.user()
        val underContextPath = CookieBrowser(mockMvc, contextPath = "/drinkit")

        // When
        val login = underContextPath.login(account.login, USER_PASSWORD).andReturn().response

        // Then
        val setCookie = login.getHeaders(HttpHeaders.SET_COOKIE).single { it.startsWith("$SESSION_COOKIE=") }
        setCookie.split(";").map { it.trim() }.drop(1) shouldContainAll
            listOf("Path=/", "Secure", "HttpOnly", "SameSite=Lax")
    }

    @Test
    fun `keeps no password hash in the session of a user the application signs in or refreshes`() {
        // Given
        val account = testAccounts.withHash("{argon2}a-hash-that-stays-with-the-accounts")

        // When
        browser.post("/api/public/sign-in-as/${account.id}").andExpect { status { isOk() } }
        browser.post("/api/cellars/refresh").andExpect { status { isOk() } }

        // Then
        attributesOf(account.id).toString() shouldNotContain "a-hash-that-stays-with-the-accounts"
    }

    @Test
    fun `ends a session 8 hours after its creation, however active`() {
        // Given
        val account = testAccounts.user()
        browser.login(account.login, USER_PASSWORD)

        // When
        age(account.id, Duration.ofHours(8).plusMinutes(1))

        // Then
        browser.get("/api/cellars/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `keeps a session younger than 8 hours`() {
        // Given
        val account = testAccounts.user()
        browser.login(account.login, USER_PASSWORD)

        // When
        age(account.id, Duration.ofHours(8).minusMinutes(1))

        // Then
        browser.get("/api/cellars/me").andExpect { status { isOk() } }
    }

    @Test
    fun `signs out, without an error, a user whose session can no longer be read`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()
        browser.login(account.login, USER_PASSWORD)

        // When
        transactions.executeWithoutResult {
            database.sql(
                """
                UPDATE drinkit_session.spring_session_attributes SET attribute_bytes = '{"@class": "com.example.Gone"}'
                WHERE session_primary_id IN (SELECT primary_id FROM drinkit_session.spring_session WHERE principal_name = ?)
                """,
            ).param(account.id).update()
        }

        // Then
        browser.get("/api/cellars/me").andExpect { status { isUnauthorized() } }
        output.out shouldContain "session_deserialization_fail"
    }

    @Test
    fun `deletes the session at logout`() {
        // Given
        val account = testAccounts.user()
        browser.login(account.login, USER_PASSWORD)

        // When
        browser.post("/api/auth/logout").andExpect { status { isNoContent() } }

        // Then
        sessionIdsOf(account.id).shouldBeEmpty()
    }

    @Test
    fun `revokes every session and remembered login of a user, and logs it`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()
        browser.login(account.login, USER_PASSWORD)
        val phone = CookieBrowser(mockMvc).apply { login(account.login, USER_PASSWORD, rememberMe = true) }
        val phoneOpenedAgain = CookieBrowser(mockMvc).apply { keep(phone.cookie(REMEMBER_ME_COOKIE).shouldNotBeNull()) }

        // When
        sessions.revokeAll(account.id)

        // Then
        sessionIdsOf(account.id).shouldBeEmpty()
        browser.get("/api/cellars/me").andExpect { status { isUnauthorized() } }
        phoneOpenedAgain.get("/api/cellars/me").andExpect { status { isUnauthorized() } }
        output.out shouldContain "session_revoked:${account.id}"
    }

    @Test
    fun `gives no session to an anonymous caller`() {
        // When
        browser.get("/api/auth/csrf")
        browser.get("/api/cellars/me")
        browser.post("/api/public/ping")

        // Then
        browser.cookie(SESSION_COOKIE).shouldBeNull()
    }

    @Test
    fun `reads back the session data of a user, with its types`() {
        // Given
        val account = testAccounts.user()
        browser.login(account.login, USER_PASSWORD)

        // When
        browser.put("/api/public/preferences").andExpect { status { isNoContent() } }

        // Then
        browser.get("/api/public/preferences").andExpect {
            jsonPath("$[0]") { value("Margaux, Pomerol") }
            jsonPath("$[1]") { value("USD") }
        }
    }

    private fun sessionIdsOf(userId: String) =
        database.sql("SELECT session_id FROM drinkit_session.spring_session WHERE principal_name = ?")
            .param(userId).query(String::class.java).list()

    private fun attributesOf(userId: String) = database.sql(
        """
        SELECT attribute_bytes::text FROM drinkit_session.spring_session_attributes
        WHERE session_primary_id IN (SELECT primary_id FROM drinkit_session.spring_session WHERE principal_name = ?)
        """,
    ).param(userId).query(String::class.java).list().map { JsonMapper.shared().readValue(it, Map::class.java) }

    private fun age(userId: String, age: Duration) = transactions.executeWithoutResult {
        database.sql(
            "UPDATE drinkit_session.spring_session SET creation_time = creation_time - ? WHERE principal_name = ?",
        ).param(age.toMillis()).param(userId).update() shouldBe 1
    }
}
