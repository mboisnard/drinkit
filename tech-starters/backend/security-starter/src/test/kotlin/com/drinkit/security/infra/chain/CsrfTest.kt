package com.drinkit.security.infra.chain

import com.drinkit.security.Browser
import com.drinkit.security.ConnectedUser
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.connectedAs
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SecuredApplicationTest
internal class CsrfTest(private val mockMvc: MockMvc) {

    private val user = ConnectedUser("user-1", setOf("ROLE_USER"))
    private val browser = Browser(mockMvc)

    @Test
    fun `refuses a POST without CSRF token`() {
        mockMvc.post("/api/public/ping").andExpect {
            status { isForbidden() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun `refuses a code request and a sign-in by code without CSRF token`() {
        listOf("/api/auth/code", "/api/auth/code/sign-in").forEach { path ->
            mockMvc.post(path) {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email": "ada@drinkit.test", "code": "123456"}"""
            }.andExpect { status { isForbidden() } }
        }
    }

    @Test
    fun `refuses a code request a browser says comes from another site`() {
        browser.post("/api/auth/code") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "ada@drinkit.test"}"""
            header("Sec-Fetch-Site", "cross-site")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `accepts the raw value of the XSRF cookie in the X-XSRF-TOKEN header`() {
        browser.post("/api/public/ping").andExpect { status { isNoContent() } }
    }

    @Test
    fun `refuses a POST a browser says comes from another site`() {
        mockMvc.post("/api/cellars") {
            with(connectedAs(user))
            header("Sec-Fetch-Site", "cross-site")
        }.andExpect {
            status { isForbidden() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun `accepts a POST a browser says comes from the same origin`() {
        mockMvc.post("/api/cellars") {
            with(connectedAs(user))
            header("Sec-Fetch-Site", "same-origin")
        }.andExpect { status { isCreated() } }
    }
}
