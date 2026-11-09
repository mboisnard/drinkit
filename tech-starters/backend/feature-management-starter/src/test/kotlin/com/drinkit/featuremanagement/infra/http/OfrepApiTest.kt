package com.drinkit.featuremanagement.infra.http

import com.drinkit.featuremanagement.FlagSubject
import com.drinkit.featuremanagement.InMemoryExperiments
import com.drinkit.featuremanagement.InMemoryFeatureFlags
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.function.RouterFunctions
import tools.jackson.databind.json.JsonMapper

internal class OfrepApiTest {

    private val ada = FlagSubject("ada")
    private val flags = InMemoryFeatureFlags(ada).apply { configure("new-checkout", enabled = true) }
    private val experiments = InMemoryExperiments(ada).apply { assignVariant("checkout-test", "B") }
    private val api = OfrepApi(flags, experiments, JsonMapper())

    private val mockMvc: MockMvc = MockMvcBuilders.routerFunctions(
        RouterFunctions.route()
            .POST(OFREP_BULK_PATH, api::evaluate)
            .POST(EXPOSURES_PATH, api::reportExposure)
            .build(),
    ).build()

    @Test
    fun `evaluates the flags and experiments the client asks for, for the subject of the request`() {
        // When
        val response = evaluate(
            """{"context": {"targetingKey": "mallory", "flags": "new-checkout", "experiments": "checkout-test"}}""",
        )

        // Then
        response.andExpect {
            status { isOk() }
            header { string("Cache-Control", "no-store, private") }
            jsonPath("$.flags[0].key") { value("new-checkout") }
            jsonPath("$.flags[0].value") { value(true) }
            jsonPath("$.flags[1].value") { value("B") }
            jsonPath("$.flags[1].reason") { value("SPLIT") }
        }

        flags.subjectsOf("new-checkout") shouldBe listOf(ada)
    }

    @Test
    fun `answers not modified when nothing changed since the client's ETag`() {
        // Given
        val etag = evaluate("""{"context": {"flags": "new-checkout"}}""").andReturn().response.getHeader("ETag")

        // When
        val response = evaluate("""{"context": {"flags": "new-checkout"}}""", ifNoneMatch = etag)

        // Then
        response.andExpect { status { isNotModified() } }
    }

    @Test
    fun `refuses a key out of kebab case, more than 50 keys, or a variant longer than 64 characters`() {
        // When
        val responses = listOf(
            evaluate("""{"context": {"flags": "New Checkout"}}"""),
            evaluate("""{"context": {"flags": "${(1..51).joinToString(",") { "flag-$it" }}"}}"""),
            reportExposure("checkout-test", "B".repeat(65)),
        )

        // Then
        responses.forAll { it.andExpect { status { isBadRequest() } } }
    }

    @Test
    fun `records an exposure only for the variant the backend assigns`() {
        // When
        reportExposure("checkout-test", "control")
        reportExposure("checkout-test", "B")

        // Then
        experiments.exposuresOf("checkout-test").map { it.subject to it.variant } shouldBe listOf(ada to "B")
    }

    private fun evaluate(json: String, ifNoneMatch: String? = null) = mockMvc.post(OFREP_BULK_PATH) {
        contentType = MediaType.APPLICATION_JSON
        content = json
        ifNoneMatch?.let { header("If-None-Match", it) }
    }

    private fun reportExposure(experiment: String, variant: String) =
        mockMvc.post("/experiments/$experiment/exposures") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"variant": "$variant"}"""
        }
}
