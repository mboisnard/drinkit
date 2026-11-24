package com.drinkit.security.infra.chain

import com.drinkit.identifier.isId
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.VisitorId
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

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
        cookie.value.isId() shouldBe true
        cookie.maxAge shouldBe THIRTY_DAYS
        cookie.isHttpOnly shouldBe true
    }

    @Test
    fun `keeps the visitor of its cookie, for 30 more days`() {
        // Given
        val visitorId = VisitorId.create().value

        // When
        val response = mockMvc.get("/api/public/visitor") { cookie(Cookie(VISITOR_COOKIE, visitorId)) }
            .andReturn().response

        // Then
        response.contentAsString shouldBe visitorId
        response.getCookie(VISITOR_COOKIE)!!.maxAge shouldBe THIRTY_DAYS
    }

    @ParameterizedTest
    @ValueSource(strings = ["<script>", "6f1d2c3e-58a4-4b7e-9d0a-1c2b3d4e5f60"])
    fun `replaces a cookie that holds no visitor id`(value: String) {
        // When
        val response = mockMvc.get("/api/public/visitor") { cookie(Cookie(VISITOR_COOKIE, value)) }
            .andReturn().response

        // Then
        response.contentAsString.isId() shouldBe true
    }

    @Test
    fun `gives the address of the client behind the request`() {
        // When
        val response = mockMvc.get("/api/public/visitor/ip-address") {
            with { request -> request.apply { remoteAddr = "203.0.113.7" } }
        }.andReturn().response

        // Then
        response.contentAsString shouldBe "203.0.113.7"
    }
}
