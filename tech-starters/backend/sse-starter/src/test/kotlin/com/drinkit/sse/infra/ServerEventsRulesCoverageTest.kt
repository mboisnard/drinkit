package com.drinkit.sse.infra

import com.drinkit.security.EndpointRules
import com.drinkit.security.EndpointRulesCoverage
import com.drinkit.security.endpointRules
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod

private const val CONTRACT = "com.drinkit.sse.contract"

internal class ServerEventsRulesCoverageTest {

    @Test
    fun `covers the stream with the rule of the starter`() {
        // When & Then
        EndpointRulesCoverage(emptyList<EndpointRules>(), CONTRACT).unruledEndpoints() shouldBe emptyList()
    }

    @Test
    fun `leaves the stream unruled when the application disables the starter`() {
        // When & Then
        EndpointRulesCoverage(emptyList<EndpointRules>(), CONTRACT, listOf("starters.sse.enabled=false"))
            .unruledEndpoints() shouldBe listOf("GET /sse/event-stream")
    }

    @Test
    fun `applies the rule of the application that replaces the starter's`() {
        // Given
        val elsewhere = endpointRules { authorize(HttpMethod.GET, "/elsewhere", permitAll) }

        // When & Then
        EndpointRulesCoverage(mapOf("serverEventsAccessRules" to elsewhere), CONTRACT)
            .unruledEndpoints() shouldBe listOf("GET /sse/event-stream")
    }
}
