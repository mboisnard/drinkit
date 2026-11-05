package com.drinkit.security.infra.login

import com.drinkit.security.Browser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.TestAccounts
import com.drinkit.security.USER_PASSWORD
import com.drinkit.security.keptXsrfCookie
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc

@SecuredApplicationTest
internal class LoginTest(mockMvc: MockMvc, accounts: InMemoryUserAccounts, encoder: PasswordEncoder) {

    private val browser = Browser(mockMvc)
    private val testAccounts = TestAccounts(accounts, encoder)

    @Test
    fun `connects a user who gives the right password, in a new session with a new XSRF token`() {
        // Given
        val account = testAccounts.user()
        val anonymousSessionId = browser.session.id
        val anonymousXsrf = browser.fetchXsrfCookie()

        // When
        val login = browser.login(account.login, USER_PASSWORD, anonymousXsrf).andExpect { status { isNoContent() } }

        // Then
        browser.session.id shouldNotBe anonymousSessionId
        val xsrf = login.andReturn().response.keptXsrfCookie().shouldNotBeNull()
        xsrf.value shouldNotBeIn listOf("", anonymousXsrf.value)
        browser.get("/api/cellars/me").andExpect { jsonPath("$.id") { value(account.id) } }
        browser.post("/api/cellars", xsrf).andExpect { status { isCreated() } }
    }

    @Test
    fun `gives the same 401 problem for an unknown login, a wrong password and an unreadable hash`() {
        // Given
        val account = testAccounts.user()
        val unprefixed = testAccounts.withHash("\$2a\$10\$szm3KQoaxzefTnJm5IlhHumS79wo/SnDJ3mYygmGrXXnrRf8cHpVC")

        // When
        val unknownLogin = browser.login("nobody@drinkit.test", USER_PASSWORD)
            .andExpect { status { isUnauthorized() } }.andReturn().response.contentAsString
        val wrongPassword = browser.login(account.login, "not the password")
            .andExpect { status { isUnauthorized() } }.andReturn().response.contentAsString
        val unreadableHash = browser.login(unprefixed.login, USER_PASSWORD)
            .andExpect { status { isUnauthorized() } }.andReturn().response.contentAsString

        // Then
        unknownLogin shouldBe wrongPassword
        unreadableHash shouldBe wrongPassword
    }

    @Test
    fun `accepts a password of 64 accented characters`() {
        // Given
        val password = "é".repeat(64)
        val account = testAccounts.user(password)

        // When
        val login = browser.login(account.login, password)

        // Then
        login.andExpect { status { isNoContent() } }
    }

    @Test
    fun `logs a successful login with the user id only`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()

        // When
        browser.login(account.login, USER_PASSWORD)

        // Then
        output.out shouldContain "authn_login_success:${account.id},method=password"
        output.out shouldNotContain account.login
    }

    @Test
    fun `logs a failed login without the login`(output: CapturedOutput) {
        // Given
        val account = testAccounts.user()

        // When
        browser.login(account.login, "not the password")

        // Then
        output.out shouldContain "authn_login_fail"
        output.out shouldNotContain account.login
    }
}
