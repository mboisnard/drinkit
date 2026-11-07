package com.drinkit.security.infra.chain

import com.drinkit.security.EndpointRulesCoverage
import com.drinkit.security.endpointRules
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod

internal class EndpointRulesCoverageTest {

    private val publicEndpoints = endpointRules {
        authorize(HttpMethod.GET, "/api/public/**", permitAll)
    }
    private val userEndpoints = endpointRules {
        authorize(HttpMethod.GET, "/api/cellars/{id}", hasRole("USER"))
    }

    @Test
    fun `reports the operations of the API that no rule declares, and only them`() {
        EndpointRulesCoverage(
            rules = listOf(publicEndpoints, userEndpoints),
            apiPackage = "com.drinkit.security.infra.chain.api",
        ).unruledEndpoints() shouldBe listOf(
            "DELETE /api/cellars/{cellarId}",
            "DELETE /api/public/ping",
            "GET /api/unruled",
        )
    }

    @Test
    fun `leaves out the operations a tech starter serves, which the starter's tests cover`() {
        EndpointRulesCoverage(
            rules = listOf(publicEndpoints, userEndpoints),
            apiPackage = "com.drinkit.security.infra.chain.api",
            servedByStarters = listOf("GET /api/unruled"),
        ).unruledEndpoints() shouldBe listOf("DELETE /api/cellars/{cellarId}", "DELETE /api/public/ping")
    }
}
