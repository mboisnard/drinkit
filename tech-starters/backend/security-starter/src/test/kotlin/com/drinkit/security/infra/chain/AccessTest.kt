package com.drinkit.security.infra.chain

import com.drinkit.security.ConnectedUser
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.connectedAs
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SecuredApplicationTest
internal class AccessTest(private val mockMvc: MockMvc) {

    private val user = ConnectedUser("user-1", setOf("ROLE_USER"))

    @Test
    fun `refuses an anonymous caller with a 401 problem`() {
        mockMvc.get("/api/cellars/42").andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(401) }
            jsonPath("$.instance") { value("/api/cellars/42") }
        }
    }

    @Test
    fun `lets a user through the rule of the application`() {
        mockMvc.get("/api/cellars/42") { with(connectedAs(user)) }.andExpect { status { isOk() } }
    }

    @Test
    fun `refuses an endpoint no rule declares, even to a user`() {
        mockMvc.get("/api/unruled") { with(connectedAs(user)) }.andExpect {
            status { isForbidden() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    fun `refuses through @PreAuthorize what the rule of the application allows`() {
        mockMvc.get("/api/admin/stats") { with(connectedAs(user)) }.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    fun `serves @PreAuthorize endpoints to the role they require`() {
        val admin = ConnectedUser("admin-1", setOf("ROLE_USER", "ROLE_ADMIN"))

        mockMvc.get("/api/admin/stats") { with(connectedAs(admin)) }.andExpect { status { isOk() } }
    }

    @Test
    fun `answers 401 when an anonymous caller asks for the current user, despite a catch-all exception handler`() {
        mockMvc.get("/api/public/me").andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
        }
    }

    @Test
    fun `gives the trace id of the request in its problem, so that its logs can be found`() {
        MDC.putCloseable("traceId", "4bf92f3577b34da6a3ce929d0e0e4736").use {
            mockMvc.get("/api/cellars/42").andExpect {
                jsonPath("$.traceId") { value("4bf92f3577b34da6a3ce929d0e0e4736") }
            }
        }
    }

    @Test
    fun `opens the health endpoint`() {
        mockMvc.get("/actuator/health").andExpect { status { isOk() } }
    }
}
