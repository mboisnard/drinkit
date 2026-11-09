package com.drinkit.sse.infra

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.IOException

private const val OPENING = ":\n\n"

private val ADA = Recipient.user("ada")
private val GRACE = Recipient.user("grace")

internal class InMemoryStreamsTest {

    private val streams = InMemoryStreams(maxStreamsPerRecipient = 2)

    @Test
    fun `opens a stream with a comment, so that the browser and the proxies see it open at once`() {
        // Given
        val tab = RecordingSseBuilder()

        // When
        streams.open(ADA, tab)

        // Then
        tab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `sends an event to every stream of its user`() {
        val firstTab = openedTab(ADA)
        val secondTab = openedTab(ADA)

        // When
        streams.send(ADA, "cellar-updated", """{"cellarId":"42"}""")

        // Then
        firstTab.sent.last() shouldBe "event:cellar-updated\ndata:{\"cellarId\":\"42\"}\n\n"
        secondTab.sent.last() shouldBe "event:cellar-updated\ndata:{\"cellarId\":\"42\"}\n\n"
    }

    @Test
    fun `never sends an event to the streams of another user`() {
        // Given
        val otherTab = openedTab(GRACE)

        // When
        streams.send(ADA, "cellar-updated", "{}")

        // Then
        otherTab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `completes the oldest stream of a user who opens one more than allowed`() {
        // Given
        val oldest = openedTab(ADA)
        val second = openedTab(ADA)
        val newest = openedTab(ADA)

        // When
        streams.send(ADA, "cellar-updated", "{}")

        // Then
        listOf(oldest.completed, second.sent.size, newest.sent.size) shouldBe listOf(true, 2, 2)
    }

    @Test
    fun `completes a stream that fails to send`() {
        // Given
        val broken = RecordingSseBuilder(
            failureAfterOpening = IOException("Broken pipe"),
        ).also { streams.open(ADA, it) }

        // When
        streams.send(ADA, "cellar-updated", "{}")

        // Then
        broken.completed shouldBe true
    }

    @Test
    fun `still sends to the other streams of a user when one fails`() {
        // Given
        streams.open(ADA, RecordingSseBuilder(failureAfterOpening = IOException("Broken pipe")))
        val tab = openedTab(ADA)

        // When
        streams.send(ADA, "cellar-updated", "{}")

        // Then
        tab.sent.size shouldBe 2
    }

    @Test
    fun `forgets the stream of a closed tab`() {
        // Given
        val tab = openedTab(ADA)
        tab.complete()

        // When
        streams.send(ADA, "cellar-updated", "{}")

        // Then
        tab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `forgets the stream of a tab whose connection failed`() {
        // Given
        val tab = openedTab(ADA)
        tab.failConnection(IOException("Connection reset"))

        // When
        streams.send(ADA, "cellar-updated", "{}")

        // Then
        tab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `completes a stream that times out, so that the browser opens it again`() {
        // Given
        val tab = openedTab(ADA)

        // When
        tab.timeOut()

        // Then
        tab.completed shouldBe true
    }

    @Test
    fun `completes at once a stream opened while the application stops`() {
        // Given
        val tab = openedTab(ADA)

        // When
        streams.closeAll()

        // Then
        listOf(tab.completed, openedTab(GRACE).completed) shouldBe listOf(true, true)
    }

    private fun openedTab(recipient: Recipient) = RecordingSseBuilder().also { streams.open(recipient, it) }
}
