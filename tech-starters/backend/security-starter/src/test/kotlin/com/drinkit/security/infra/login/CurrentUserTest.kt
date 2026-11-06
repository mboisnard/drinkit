package com.drinkit.security.infra.login

import com.drinkit.security.Browser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.TestAccounts
import com.drinkit.security.keptXsrfCookie
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.test.web.servlet.MockMvc

@SecuredApplicationTest
internal class CurrentUserTest(
    mockMvc: MockMvc,
    private val accounts: InMemoryUserAccounts,
    codes: SpySignInCodeSender,
) {

    private val browser = Browser(mockMvc, codes)
    private val testAccounts = TestAccounts(accounts)

    @Test
    fun `gives the roles of the user, without the factors of its login`() {
        // Given
        val account = testAccounts.user()
        browser.signIn(account.login)

        // When
        val me = browser.get("/api/cellars/me")

        // Then
        me.andExpect {
            jsonPath("$.roles.length()") { value(1) }
            jsonPath("$.roles[0]") { value("ROLE_USER") }
        }
    }

    @Test
    fun `signs a just registered user in, in a new session with a new XSRF token`() {
        // Given
        val account = testAccounts.user()
        val anonymousSessionId = browser.session.id
        val anonymousXsrf = browser.fetchXsrfCookie()

        // When
        val signIn = browser.post("/api/public/sign-in-as/${account.id}", anonymousXsrf).andExpect { status { isOk() } }

        // Then
        browser.session.id shouldNotBe anonymousSessionId
        signIn.andReturn().response.keptXsrfCookie().shouldNotBeNull().value shouldNotBeIn
            listOf("", anonymousXsrf.value)
        browser.get("/api/cellars/me").andExpect { jsonPath("$.id") { value(account.id) } }
    }

    @Test
    fun `logs a sign in with its own method`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()

        // When
        browser.post("/api/public/sign-in-as/${account.id}")

        // Then
        output.out shouldContain "authn_login_success:${account.id},method=sign_in"
    }

    @Test
    fun `reloads the roles of the user at refresh, in a new session with a new XSRF token`() {
        // Given
        val account = testAccounts.user()
        browser.signIn(account.login)
        val sessionId = browser.session.id
        val xsrf = browser.fetchXsrfCookie()
        val admin = accounts.findById(account.id).shouldNotBeNull().copy(roles = setOf("ROLE_USER", "ROLE_ADMIN"))
        accounts.save(account.login, admin)

        // When
        val refresh = browser.post("/api/cellars/refresh", xsrf).andExpect { status { isOk() } }

        // Then
        browser.session.id shouldNotBe sessionId
        refresh.andReturn().response.keptXsrfCookie().shouldNotBeNull().value shouldNotBeIn listOf("", xsrf.value)
        browser.get("/api/admin/stats").andExpect { status { isOk() } }
    }

    @Test
    fun `signs out at refresh a user whose account is gone`() {
        // Given
        val account = testAccounts.user()
        browser.signIn(account.login)
        accounts.remove(account.id)

        // When
        browser.post("/api/cellars/refresh").andExpect { status { isOk() } }

        // Then
        browser.get("/api/cellars/me").andExpect { status { isUnauthorized() } }
    }
}
