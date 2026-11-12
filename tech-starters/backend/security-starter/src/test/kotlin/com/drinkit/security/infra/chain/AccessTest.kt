package com.drinkit.security.infra.chain

import com.drinkit.security.ConnectedUser
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.connectedAs
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SecuredApplicationTest
internal class AccessTest(private val mockMvc: MockMvc) {

    private val user = ConnectedUser("user-1", setOf("ROLE_USER"))

    @Test
    fun `refuses an anonymous caller with a 401 problem`() {
        // When
        val response = mockMvc.get("/api/cellars/42")

        // Then
        response.andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(401) }
            jsonPath("$.instance") { value("/api/cellars/42") }
        }
    }

    @Test
    fun `lets a user through the rule of the application`() {
        // When
        val response = mockMvc.get("/api/cellars/42") { with(connectedAs(user)) }

        // Then
        response.andExpect { status { isOk() } }
    }

    @Test
    fun `refuses an endpoint no rule declares, even to a user`() {
        // When
        val response = mockMvc.get("/api/unruled") { with(connectedAs(user)) }

        // Then
        response.andExpect {
            status { isForbidden() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    fun `refuses through @PreAuthorize what the rule of the application allows`() {
        // When
        val response = mockMvc.get("/api/admin/stats") { with(connectedAs(user)) }

        // Then
        response.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    fun `serves @PreAuthorize endpoints to the role they require`() {
        // Given
        val admin = ConnectedUser("admin-1", setOf("ROLE_USER", "ROLE_ADMIN"))

        // When
        val response = mockMvc.get("/api/admin/stats") { with(connectedAs(admin)) }

        // Then
        response.andExpect { status { isOk() } }
    }

    @Test
    fun `answers 401 when an anonymous caller asks for the current user, despite a catch-all exception handler`() {
        // When
        val response = mockMvc.get("/api/public/me")

        // Then
        response.andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
        }
    }

    @Test
    fun `lets an anonymous probe read the health, even under a rule keeping the actuator to admins`() {
        // When
        val response = mockMvc.get("/actuator/health")

        // Then
        response.andExpect { status { isOk() } }
    }

    @Test
    fun `refuses the other actuator endpoints to an anonymous caller`() {
        // When
        val response = mockMvc.get("/actuator/info")

        // Then
        response.andExpect { status { isUnauthorized() } }
    }
}
