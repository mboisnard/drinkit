package com.drinkit.sse.infra

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
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
        val tab = RecordingSseBuilder()

        streams.open(ADA, tab)

        tab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `sends an event to every stream of its user`() {
        val firstTab = openedTab(ADA)
        val secondTab = openedTab(ADA)

        streams.send(ADA, "cellar-updated", """{"cellarId":"42"}""")

        firstTab.sent.last() shouldBe "event:cellar-updated\ndata:{\"cellarId\":\"42\"}\n\n"
        secondTab.sent.last() shouldBe "event:cellar-updated\ndata:{\"cellarId\":\"42\"}\n\n"
    }

    @Test
    fun `never sends an event to the streams of another user`() {
        val otherTab = openedTab(GRACE)

        streams.send(ADA, "cellar-updated", "{}")

        otherTab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `completes the oldest stream of a user who opens one more than allowed`() {
        val oldest = openedTab(ADA)
        val second = openedTab(ADA)
        val newest = openedTab(ADA)

        listOf(oldest.completed, second.completed, newest.completed) shouldBe listOf(true, false, false)
    }

    @Test
    fun `completes a stream that fails to send`() {
        val broken = RecordingSseBuilder(failure = IOException("Broken pipe"))
        streams.open(ADA, broken)

        streams.send(ADA, "cellar-updated", "{}")

        broken.completed shouldBe true
    }

    @Test
    fun `still sends to the other streams of the user when one fails`() {
        streams.open(ADA, RecordingSseBuilder(failure = IOException("Broken pipe")))
        val tab = openedTab(ADA)

        streams.send(ADA, "cellar-updated", "{}")

        tab.sent shouldHaveSize 2
    }

    @Test
    fun `forgets a stream once it completes`() {
        val tab = openedTab(ADA)
        tab.complete()

        streams.send(ADA, "cellar-updated", "{}")

        tab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `forgets a stream once it fails`() {
        val tab = openedTab(ADA)
        tab.failWith(IOException("Connection reset"))

        streams.send(ADA, "cellar-updated", "{}")

        tab.sent shouldContainExactly listOf(OPENING)
    }

    @Test
    fun `completes a stream that times out, so that the browser opens it again`() {
        val tab = openedTab(ADA)

        tab.timeOut()

        tab.completed shouldBe true
    }

    @Test
    fun `pings every open stream with a comment`() {
        val firstTab = openedTab(ADA)
        val otherTab = openedTab(GRACE)

        streams.ping()

        firstTab.sent shouldContainExactly listOf(OPENING, OPENING)
        otherTab.sent shouldContainExactly listOf(OPENING, OPENING)
    }

    @Test
    fun `completes every open stream`() {
        val firstTab = openedTab(ADA)
        val otherTab = openedTab(GRACE)

        streams.completeAll()

        listOf(firstTab.completed, otherTab.completed) shouldBe listOf(true, true)
    }

    private fun openedTab(recipient: Recipient) = RecordingSseBuilder().also { streams.open(recipient, it) }
}
