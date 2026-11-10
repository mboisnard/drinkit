package com.drinkit.security

import com.drinkit.security.infra.code.InMemorySignInCodes
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
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
    classes = [SecurityTestApplication::class, InMemoryCodes::class],
    // Sessions in memory, as MockMvc keeps them: PostgresSessionsTest keeps them in PostgreSQL
    properties = [
        "spring.autoconfigure.exclude=$IN_MEMORY_SESSIONS",
        "starters.postgresql.enabled=false",
        "starters.security.code.max-email-length=100",
    ],
)
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(OutputCaptureExtension::class)
internal annotation class SecuredApplicationTest

// Without PostgreSQL, the test gives the codes the starter would keep there, as PostgresSessionsTest does not
@TestConfiguration(proxyBeanMethods = false)
internal class InMemoryCodes {

    @Bean
    fun signInCodes() = InMemorySignInCodes()
}

/**
 * A browser of the test application: it keeps its session, sends the XSRF cookie back in its header, and reads the
 * sign-in codes its user gets from [codes].
 */
internal class Browser(private val mockMvc: MockMvc, private val codes: SpySignInCodeSender = SpySignInCodeSender()) {

    var session = MockHttpSession()

    // As a browser sends its session cookie: without it, the session id is not the requested one, and stays
    private fun MockHttpServletRequestDsl.withSession() {
        session = this@Browser.session
        with { it.apply { requestedSessionId = this@Browser.session.id } }
    }

    fun fetchXsrfCookie(): Cookie = requireNotNull(
        mockMvc.get("/api/auth/csrf") { withSession() }.andReturn().response.getCookie(XSRF_COOKIE),
    )

    fun requestCode(login: String, xsrf: Cookie = fetchXsrfCookie()): ResultActionsDsl = post("/api/auth/code", xsrf) {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email": "$login"}"""
    }

    fun signInWithCode(login: String, code: String, xsrf: Cookie = fetchXsrfCookie()): ResultActionsDsl =
        post("/api/auth/code/sign-in", xsrf) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "$login", "code": "$code"}"""
        }

    /** Asks for a code for [login], then signs in with the code its email got, as a user does. */
    fun signIn(login: String, xsrf: Cookie = fetchXsrfCookie()): ResultActionsDsl {
        requestCode(login, xsrf).andExpect { status { isNoContent() } }
        return signInWithCode(login, codes.lastCodeOf(login), xsrf)
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

/** A code of six digits other than this one. */
internal fun String.wrongCode() = ((toInt() + 1) % 1_000_000).toString().padStart(6, '0')

/** The XSRF cookie a browser keeps from this response: the last one it sets, after any that expires the previous. */
internal fun MockHttpServletResponse.keptXsrfCookie(): Cookie? = cookies.lastOrNull { it.name == XSRF_COOKIE }

/** Accounts of the test application, each test with its own. */
internal class TestAccounts(private val accounts: InMemoryUserAccounts) {

    fun user(roles: Set<String> = setOf("ROLE_USER"), login: String? = null): TestAccount {
        val id = UUID.randomUUID().toString()
        val account = TestAccount(id, login ?: "$id@drinkit.test")
        accounts.save(account.login, UserAccount(id, roles))
        return account
    }
}

internal data class TestAccount(val id: String, val login: String)
