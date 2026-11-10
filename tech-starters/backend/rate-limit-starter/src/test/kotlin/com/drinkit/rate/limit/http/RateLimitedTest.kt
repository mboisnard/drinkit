package com.drinkit.rate.limit.http

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.rate.limit.generated.jooq.DrinkitApplication
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@SpringBootTest(classes = [RateLimitTestApplication::class])
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class RateLimitedTest(private val mockMvc: MockMvc) {

    @Test
    fun `an annotated endpoint answers 429 with Retry-After once its limit is reached`() {
        // Given
        repeat(2) { mockMvc.get("/limited/catalog") }

        // When
        val third = mockMvc.get("/limited/catalog")

        // Then
        third.andExpect {
            status { isTooManyRequests() }
            header { string("Retry-After", "1800") }
        }
    }

    @Test
    fun `the policy reads its key in the method arguments`() {
        // Given
        mockMvc.get("/limited/codes") { param("email", "Ada@Drinkit.com") }

        // When
        val sameEmail = mockMvc.get("/limited/codes") { param("email", "ada@drinkit.com") }
        val otherEmail = mockMvc.get("/limited/codes") { param("email", "grace@drinkit.com") }

        // Then
        sameEmail.andExpect { status { isTooManyRequests() } }
        otherEmail.andExpect { status { isOk() } }
    }

    @Test
    fun `a policy that gives no key lets the call through uncounted`() {
        // Given
        mockMvc.get("/limited/reports")

        // When
        val again = mockMvc.get("/limited/reports")

        // Then
        again.andExpect { status { isOk() } }
    }

    @Test
    fun `a count made inside a transaction survives its rollback`() {
        // Given
        mockMvc.get("/limited/imports").andExpect { status { isConflict() } }

        // When
        val retry = mockMvc.get("/limited/imports")

        // Then
        retry.andExpect { status { isTooManyRequests() } }
        retry.andReturn().response.getHeader("Retry-After") shouldBe "3600"
    }
}
