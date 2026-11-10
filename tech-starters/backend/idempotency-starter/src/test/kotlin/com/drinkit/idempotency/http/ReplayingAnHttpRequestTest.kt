package com.drinkit.idempotency.http

import com.drinkit.idempotency.generated.jooq.DrinkitApplication
import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.ConnectedUser
import com.drinkit.security.connectedAs
import com.drinkit.security.generated.jooq.DrinkitSession
import com.drinkit.test.ControlledClock
import com.drinkit.test.rootCause
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@JooqIntegrationTest(schemas = [DrinkitSession::class, DrinkitApplication::class])
@SpringBootTest(classes = [IdempotencyTestApplication::class])
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class ReplayingAnHttpRequestTest(
    private val mockMvc: MockMvc,
    private val cellars: Cellars,
    private val clock: ControlledClock,
) {

    private val ada = ConnectedUser("ada", setOf("ROLE_USER"))
    private val grace = ConnectedUser("grace", setOf("ROLE_USER"))
    private val key = UUID.randomUUID().toString()

    @BeforeEach
    fun reset() {
        cellars.created.clear()
        cellars.nextAnswers.clear()
        cellars.pause = null
        cellars.refused = false
        clock.reset()
    }

    @Test
    fun `a retried request gets the first answer back without running again`() {
        // Given
        createCellar("Cave du Rhône", key)

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect {
            status { isCreated() }
            header { string("Location", "/cellars/cellar-1") }
            content { string("cellar-1") }
        }
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a replay never carries the cookies of the first answer`() {
        // Given
        createCellar("Cave du Rhône", key)

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect { cookie { doesNotExist("last-cellar") } }
    }

    @Test
    fun `a retry while the first request runs is told to come back later`() {
        // Given
        val pause = Cellars.Pause().also { cellars.pause = it }
        val first = CompletableFuture.supplyAsync { createCellar("Cave du Rhône", key).andReturn().response.status }
        pause.started.await(5, TimeUnit.SECONDS)
        cellars.pause = null

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect {
            status { isConflict() }
            header { exists("Retry-After") }
        }
        pause.resume()
        first.get(5, TimeUnit.SECONDS) shouldBe 201
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a key reused with another body is refused`() {
        // Given
        createCellar("Cave du Rhône", key)

        // When
        val other = createCellar("Cave de Loire", key)

        // Then
        other.andExpect { status { isUnprocessableContent() } }
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a key belongs to its user, two users never share one`() {
        // Given
        createCellar("Cave du Rhône", key, user = ada)

        // When
        createCellar("Cave du Rhône", key, user = grace)

        // Then
        cellars.created shouldContainExactly listOf("Cave du Rhône", "Cave du Rhône")
    }

    @Test
    fun `a request outliving its lease keeps nothing once a retry took its key over`() {
        // Given
        clock.fix()
        val pause = Cellars.Pause().also { cellars.pause = it }
        val first = CompletableFuture.supplyAsync { runCatching { createCellar("Cave du Rhône", key) } }
        pause.started.await(5, TimeUnit.SECONDS)
        cellars.pause = null
        clock.add(Duration.ofMinutes(5).plusSeconds(1))

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect { status { isCreated() } }
        pause.resume()
        first.get(5, TimeUnit.SECONDS).isFailure shouldBe true
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a refusal whose transaction rolls back frees the key for the retry`() {
        // Given
        cellars.refused = true
        createCellar("Cave du Rhône", key).andExpect { status { isBadRequest() } }
        cellars.refused = false

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect { status { isCreated() } }
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a 5xx answer rolls its writes back and frees the key`() {
        // Given
        cellars.nextAnswers += ResponseEntity.internalServerError().build()
        createCellar("Cave du Rhône", key).andExpect { status { isInternalServerError() } }

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect { status { isCreated() } }
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a 429 answer rolls its writes back and frees the key`() {
        // Given
        cellars.nextAnswers += ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        createCellar("Cave du Rhône", key).andExpect { status { isTooManyRequests() } }

        // When
        val retry = createCellar("Cave du Rhône", key)

        // Then
        retry.andExpect { status { isCreated() } }
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `an answer over 64 KB fails and rolls its writes back`() {
        // Given
        cellars.nextAnswers += ResponseEntity.ok("a".repeat(64 * 1024 + 1))

        // When
        val creation = runCatching { createCellar("Cave du Rhône", key) }

        // Then
        creation.exceptionOrNull()?.rootCause()?.message shouldBe
            "An @Idempotent endpoint answers at most 64 KB, which a retry gets back"
        cellars.created shouldBe emptyList()
    }

    @Test
    fun `a key reused with another query string is refused`() {
        // Given
        mockMvc.post("/cellars?region=rhone") { cellar("Cave du Rhône", key, ada) }

        // When
        val retry = mockMvc.post("/cellars?region=loire") { cellar("Cave du Rhône", key, ada) }

        // Then
        retry.andExpect { status { isUnprocessableContent() } }
        cellars.created shouldContainExactly listOf("Cave du Rhône")
    }

    @Test
    fun `a blank key or one over 255 characters is refused`() {
        // When
        val blank = createCellar("Cave du Rhône", " ")
        val tooLong = createCellar("Cave du Rhône", "k".repeat(256))

        // Then
        blank.andExpect { status { isBadRequest() } }
        tooLong.andExpect { status { isBadRequest() } }
        cellars.created shouldBe emptyList()
    }

    @Test
    fun `a keyed request over 64 KB is refused before it is read whole`() {
        // When
        val creation = createCellar("a".repeat(64 * 1024), key)

        // Then
        creation.andExpect { status { isPayloadTooLarge() } }
        cellars.created shouldBe emptyList()
    }

    @Test
    fun `a request without a key runs each time`() {
        // Given
        createCellar("Cave du Rhône", key = null)

        // When
        createCellar("Cave du Rhône", key = null)

        // Then
        cellars.created shouldContainExactly listOf("Cave du Rhône", "Cave du Rhône")
    }

    @Test
    fun `an endpoint that is not idempotent ignores the key`() {
        // Given
        mockMvc.post("/cellars/tasting") { cellar("Cave du Rhône", key, ada) }

        // When
        mockMvc.post("/cellars/tasting") { cellar("Cave du Rhône", key, ada) }

        // Then
        cellars.created shouldContainExactly listOf("Cave du Rhône", "Cave du Rhône")
    }

    private fun createCellar(name: String, key: String?, user: ConnectedUser = ada): ResultActionsDsl =
        mockMvc.post("/cellars") { cellar(name, key, user) }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.cellar(
        name: String,
        key: String?,
        user: ConnectedUser,
    ) {
        with(connectedAs(user))
        contentType = MediaType.APPLICATION_JSON
        content = """{"name": "$name"}"""
        key?.let { header("Idempotency-Key", it) }
    }
}
