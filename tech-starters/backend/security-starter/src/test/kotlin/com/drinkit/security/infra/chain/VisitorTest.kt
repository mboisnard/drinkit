package com.drinkit.security.infra.chain

import com.drinkit.security.SecuredApplicationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

private const val VISITOR_COOKIE = "__Host-visitor"
private const val THIRTY_DAYS = 30 * 24 * 60 * 60

@SecuredApplicationTest
internal class VisitorTest(private val mockMvc: MockMvc) {

    @Test
    fun `gives a first visit its visitor, kept 30 days by an HttpOnly cookie`() {
        // When
        val response = mockMvc.get("/api/public/visitor").andReturn().response

        // Then
        val cookie = response.getCookie(VISITOR_COOKIE)!!
        response.contentAsString shouldBe cookie.value
        cookie.maxAge shouldBe THIRTY_DAYS
        cookie.isHttpOnly shouldBe true
    }

    @Test
    fun `keeps the visitor of its cookie, for 30 more days`() {
        // Given
        val visitorId = UUID.randomUUID().toString()

        // When
        val response = mockMvc.get("/api/public/visitor") { cookie(Cookie(VISITOR_COOKIE, visitorId)) }
            .andReturn().response

        // Then
        response.contentAsString shouldBe visitorId
        response.getCookie(VISITOR_COOKIE)!!.maxAge shouldBe THIRTY_DAYS
    }

    @Test
    fun `replaces a cookie that holds no visitor id`() {
        // When
        val response = mockMvc.get("/api/public/visitor") { cookie(Cookie(VISITOR_COOKIE, "<script>")) }
            .andReturn().response

        // Then
        response.contentAsString shouldNotBe "<script>"
        UUID.fromString(response.contentAsString)
    }
}
