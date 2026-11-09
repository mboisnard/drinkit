package com.drinkit.event.sourcing

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SequenceIdTest {

    @Test
    fun `starts at 0`() {
        // Given & When
        val sequenceId = SequenceId()

        // Then
        sequenceId.value shouldBe 0
    }

    @Test
    fun `leaves a gap after a sequence id, so that an event can be inserted in between`() {
        // Given
        val sequenceId = SequenceId(10)

        // When
        val nextSequenceId = sequenceId.next()

        // Then
        nextSequenceId.value shouldBe 20 // 10 + INTERVAL_BETWEEN_SEQUENCES
    }

    @Test
    fun `orders sequence ids by their value`() {
        // Given
        val sequenceId1 = SequenceId(10)
        val sequenceId2 = SequenceId(20)
        val sequenceId3 = SequenceId(10)

        // When & Then
        (sequenceId1 < sequenceId2) shouldBe true
        (sequenceId2 > sequenceId1) shouldBe true
        (sequenceId1 == sequenceId3) shouldBe true
        (sequenceId1 <= sequenceId3) shouldBe true
        (sequenceId1 >= sequenceId3) shouldBe true
    }
}
