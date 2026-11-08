package com.drinkit.security.infra.chain

import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.XSRF_COOKIE
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get

@SecuredApplicationTest
internal class CookiesAndHeadersTest(private val mockMvc: MockMvc) {

    @Test
    fun `sets the XSRF cookie for the whole site, even under a context path`() {
        // When
        val response = mockMvc.get("/drinkit/api/auth/csrf") { contextPath = "/drinkit" }.andReturn().response

        // Then
        val setCookie = response.getHeaders(HttpHeaders.SET_COOKIE).first { it.startsWith("$XSRF_COOKIE=") }
        setCookie.attributes() shouldContainAll listOf("Path=/", "Secure")
        setCookie.attributes() shouldNotContain "HttpOnly"
        // The mock response leaves SameSite out of the header it formats, which Tomcat writes
        response.getCookie(XSRF_COOKIE)?.getAttribute("SameSite") shouldBe "Lax"
    }

    @Test
    fun `sends the security headers over https`() {
        mockMvc.get("/api/auth/csrf") { secure = true }.andExpectSecurityHeaders()
    }

    @Test
    fun `sends the security headers over http, as behind a proxy that ends TLS`() {
        mockMvc.get("/api/auth/csrf") { secure = false }.andExpectSecurityHeaders()
    }

    private fun ResultActionsDsl.andExpectSecurityHeaders() = andExpect {
        header {
            string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
            string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains")
            string("Referrer-Policy", "strict-origin-when-cross-origin")
            string("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
            string("Cross-Origin-Opener-Policy", "same-origin")
            string("Cross-Origin-Resource-Policy", "same-origin")
            string("X-Content-Type-Options", "nosniff")
            string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
        }
    }

    private fun String.attributes() = split(";").map { it.trim() }.drop(1)
}
