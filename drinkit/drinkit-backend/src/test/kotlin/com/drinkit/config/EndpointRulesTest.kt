package com.drinkit.config

import com.drinkit.security.EndpointRulesCoverage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class EndpointRulesTest {

    @Test
    fun `every operation of the API contract has an access rule`() {
        EndpointRulesCoverage(
            rules = listOf(SecurityConfiguration().drinkitEndpoints()),
            apiPackage = "com.drinkit.api.generated.api",
            servedByStarters = listOf("GET /sse/event-stream"),
        ).unruledEndpoints() shouldBe emptyList()
    }
}
