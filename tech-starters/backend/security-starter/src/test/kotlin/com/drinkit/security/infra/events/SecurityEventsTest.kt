package com.drinkit.security.infra.events

import com.drinkit.security.ConnectedUser
import com.drinkit.security.SecuredApplicationTest
import com.drinkit.security.connectedAs
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SecuredApplicationTest
internal class SecurityEventsTest(private val mockMvc: MockMvc) {

    @Test
    fun `logs a refusal by @PreAuthorize with the user id`(output: CapturedOutput) {
        // When
        mockMvc.get("/api/admin/stats") { with(connectedAs(ConnectedUser("user-7", setOf("ROLE_USER")))) }

        // Then
        output.out shouldContain "authz_fail:user-7"
    }

    @Test
    fun `logs no refusal of an anonymous caller`(output: CapturedOutput) {
        // When
        mockMvc.get("/api/cellars/42")

        // Then
        output.out shouldNotContain "authz_fail"
    }
}
