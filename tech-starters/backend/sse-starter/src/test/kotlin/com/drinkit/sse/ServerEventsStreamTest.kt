package com.drinkit.sse

import com.drinkit.postgresql.JooqIntegrationTest
import com.drinkit.security.ConnectedUser
import com.drinkit.security.connectedAs
import com.drinkit.security.generated.jooq.DrinkitSecurity
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import jakarta.servlet.http.Cookie
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.Base64
import kotlin.time.Duration.Companion.seconds

@JooqIntegrationTest(schemas = [DrinkitSecurity::class])
@SpringBootTest(classes = [SseTestApplication::class])
// The database of the class is dropped after it, so no other class can reuse the context that points at it
@DirtiesContext
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class ServerEventsStreamTest(private val mockMvc: MockMvc, private val serverEvents: ServerEvents) {

    private val ada = ConnectedUser("ada", setOf("ROLE_USER"))

    @Test
    fun `tells a visitor without a session to stop asking for the stream`() {
        // When & Then
        mockMvc.get("/sse/event-stream").andExpect {
            status { isNoContent() }
            cookie { doesNotExist(SESSION_COOKIE) }
        }
    }

    @Test
    fun `sends an event to the open stream of a session`() {
        // Given
        val session = visit()
        val stream = mockMvc.get("/sse/event-stream") { cookie(session) }.andReturn().response
        val sessionId = String(Base64.getDecoder().decode(session.value))

        // When
        serverEvents.sendToSession(sessionId, "price-changed", mapOf("bottleId" to "7"))

        // Then
        runBlocking {
            eventually(2.seconds) {
                stream.contentAsString shouldContain "event:price-changed\ndata: {\"bottleId\":\"7\"}\n\n"
            }
        }
    }

    @Test
    fun `sends an event to the open stream of its user`() {
        // Given
        val stream = mockMvc.get("/sse/event-stream") { with(connectedAs(ada)) }.andReturn().response

        // When
        serverEvents.send("ada", "cellar-updated", mapOf("cellarId" to "42"))

        // Then
        runBlocking {
            eventually(2.seconds) {
                stream.contentAsString shouldContain "event:cellar-updated\ndata: {\"cellarId\":\"42\"}\n\n"
            }
        }
    }

    @Test
    fun `opens a stream with the id of the last event its user got, so that a tab knows it missed none`() {
        // Given
        val grace = ConnectedUser("grace", setOf("ROLE_USER"))
        val firstTab = mockMvc.get("/sse/event-stream") { with(connectedAs(grace)) }.andReturn().response
        serverEvents.send("grace", "cellar-updated", mapOf("cellarId" to "42"))
        val sentEvent = Regex("id:(.+)\nevent:cellar-updated")
        val eventId = runBlocking {
            eventually(2.seconds) { checkNotNull(sentEvent.find(firstTab.contentAsString)) }
        }.groupValues[1]

        // When
        val secondTab = mockMvc.get("/sse/event-stream") { with(connectedAs(grace)) }.andReturn().response

        // Then
        secondTab.contentAsString shouldStartWith "id:$eventId\nevent:init\n"
    }

    private fun visit(): Cookie = checkNotNull(mockMvc.get("/visit").andReturn().response.getCookie(SESSION_COOKIE))
}

private const val SESSION_COOKIE = "__Host-SESSION"
