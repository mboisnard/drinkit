package com.drinkit.security

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

const val XSRF_COOKIE = "__Host-XSRF-TOKEN"

private const val IN_MEMORY_SESSIONS = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"

@Target(AnnotationTarget.CLASS)
@SpringBootTest(
    classes = [SecurityTestApplication::class],
    // Sessions in memory, as MockMvc keeps them: PostgresSessionsTest keeps them in PostgreSQL
    properties = ["spring.autoconfigure.exclude=$IN_MEMORY_SESSIONS", "starters.postgresql.enabled=false"],
)
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(OutputCaptureExtension::class)
internal annotation class SecuredApplicationTest

/** A browser of the test application: it keeps its session, and sends the XSRF cookie back in its header. */
internal class Browser(private val mockMvc: MockMvc) {

    var session = MockHttpSession()

    // As a browser sends its session cookie: without it, the session id is not the requested one, and stays
    private fun MockHttpServletRequestDsl.withSession() {
        session = this@Browser.session
        with { it.apply { requestedSessionId = this@Browser.session.id } }
    }

    fun fetchXsrfCookie(): Cookie = requireNotNull(
        mockMvc.get("/api/auth/csrf") { withSession() }.andReturn().response.getCookie(XSRF_COOKIE),
    )

    fun login(login: String, password: String, xsrf: Cookie = fetchXsrfCookie()): ResultActionsDsl =
        post("/api/auth/login", xsrf) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username": "$login", "password": "$password"}"""
        }

    fun get(path: String, dsl: MockHttpServletRequestDsl.() -> Unit = {}) = mockMvc.get(path) {
        withSession()
        dsl()
    }

    fun post(
        path: String,
        xsrf: Cookie = fetchXsrfCookie(),
        dsl: MockHttpServletRequestDsl.() -> Unit = {},
    ): ResultActionsDsl = mockMvc.post(path) {
        withSession()
        cookie(xsrf)
        header("X-XSRF-TOKEN", xsrf.value)
        dsl()
    }
}

/** The XSRF cookie a browser keeps from this response: the last one it sets, after any that expires the previous. */
internal fun MockHttpServletResponse.keptXsrfCookie(): Cookie? = cookies.lastOrNull { it.name == XSRF_COOKIE }

/** Accounts of the test application, each test with its own. */
internal class TestAccounts(private val accounts: InMemoryUserAccounts, private val encoder: PasswordEncoder) {

    fun user(password: String = USER_PASSWORD, roles: Set<String> = setOf("ROLE_USER"), login: String? = null) =
        withHash(encoder.encode(password)!!, roles, login)

    fun withHash(passwordHash: String, roles: Set<String> = setOf("ROLE_USER"), login: String? = null): TestAccount {
        val id = UUID.randomUUID().toString()
        val account = TestAccount(id, login ?: "$id@drinkit.test")
        accounts.save(account.login, UserAccount(id, passwordHash, roles))
        return account
    }
}

internal data class TestAccount(val id: String, val login: String)
