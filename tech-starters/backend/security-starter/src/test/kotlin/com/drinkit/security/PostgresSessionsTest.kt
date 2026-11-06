package com.drinkit.security

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.request

const val SESSION_COOKIE = "__Host-SESSION"
const val REMEMBER_ME_COOKIE = "__Host-remember-me"

/**
 * The test application with its sessions in PostgreSQL, as the application keeps them. The test class carries
 * `@JooqIntegrationTest` first, which builds its database from the jOOQ classes: the extension reads no
 * meta-annotation.
 */
@Target(AnnotationTarget.CLASS)
@SpringBootTest(classes = [SecurityTestApplication::class])
// The database of each test class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(OutputCaptureExtension::class)
internal annotation class PostgresSessionsTest

/** A browser of the test application: it keeps the cookies it is given, and sends the XSRF cookie in its header. */
internal class CookieBrowser(private val mockMvc: MockMvc, private val contextPath: String = "") {

    private val cookies = mutableMapOf<String, Cookie>()

    fun cookie(name: String): Cookie? = cookies[name]

    fun forget(name: String) {
        cookies.remove(name)
    }

    fun keep(cookie: Cookie) {
        cookies[cookie.name] = cookie
    }

    /** Asks for a code for [login], then signs in with the code [codes] got, as a user does with their email. */
    fun signIn(
        login: String,
        codes: SpySignInCodeSender,
        rememberMe: Boolean = false,
        dsl: MockHttpServletRequestDsl.() -> Unit = {},
    ): ResultActionsDsl {
        post("/api/auth/code") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "$login"}"""
            dsl()
        }
        return post("/api/auth/code/sign-in") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "$login", "code": "${codes.lastCodeOf(login)}", "rememberMe": $rememberMe}"""
            dsl()
        }
    }

    fun get(path: String, dsl: MockHttpServletRequestDsl.() -> Unit = {}) = send(HttpMethod.GET, path, dsl)

    fun post(path: String, dsl: MockHttpServletRequestDsl.() -> Unit = {}) = sendWithXsrf(HttpMethod.POST, path, dsl)

    fun put(path: String, dsl: MockHttpServletRequestDsl.() -> Unit = {}) = sendWithXsrf(HttpMethod.PUT, path, dsl)

    private fun sendWithXsrf(
        method: HttpMethod,
        path: String,
        dsl: MockHttpServletRequestDsl.() -> Unit,
    ): ResultActionsDsl {
        val xsrf = cookies[XSRF_COOKIE] ?: get("/api/auth/csrf").let { requireNotNull(cookies[XSRF_COOKIE]) }
        return send(method, path) {
            header("X-XSRF-TOKEN", xsrf.value)
            dsl()
        }
    }

    private fun send(method: HttpMethod, path: String, dsl: MockHttpServletRequestDsl.() -> Unit): ResultActionsDsl {
        val result = mockMvc.request(method, contextPath + path) {
            contextPath = this@CookieBrowser.contextPath
            if (this@CookieBrowser.cookies.isNotEmpty()) cookie(*this@CookieBrowser.cookies.values.toTypedArray())
            dsl()
        }

        result.andReturn().response.cookies.forEach { cookie ->
            if (cookie.maxAge == 0) cookies.remove(cookie.name) else cookies[cookie.name] = cookie
        }

        return result
    }
}
