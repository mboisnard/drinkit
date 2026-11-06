package com.drinkit.security.infra.login

import com.drinkit.security.Browser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.TestAccounts
import com.drinkit.security.XSRF_COOKIE
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc

@SecuredApplicationTest
internal class LogoutTest(mockMvc: MockMvc, accounts: InMemoryUserAccounts, codes: SpySignInCodeSender) {

    private val browser = Browser(mockMvc, codes)
    private val account = TestAccounts(accounts).user()

    @Test
    fun `ends the session, expires its cookies for the whole site, and clears the cache and storage of the browser`() {
        // Given
        browser.signIn(account.login)

        // When
        val logout = browser.post("/api/auth/logout") { secure = true }.andExpect { status { isNoContent() } }

        // Then
        browser.session.isInvalid shouldBe true
        val setCookies = logout.andReturn().response.getHeaders(HttpHeaders.SET_COOKIE)
        val expired = setCookies.map { it.split("; ") }.filter { it.containsAll(listOf("Path=/", "Max-Age=0")) }
        expired.map { it.first().substringBefore('=') } shouldContainExactlyInAnyOrder
            listOf("__Host-SESSION", XSRF_COOKIE, "__Host-remember-me")
        logout.andExpect { header { string("Clear-Site-Data", "\"cache\", \"storage\"") } }
    }
}
