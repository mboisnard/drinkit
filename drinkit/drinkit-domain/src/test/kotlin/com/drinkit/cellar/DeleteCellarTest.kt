package com.drinkit.cellar

import com.drinkit.cellar.DeleteCellar.Result.CellarDeleted
import com.drinkit.cellar.DeleteCellar.Result.CellarNotFound
import com.drinkit.cellar.core.Cellar
import com.drinkit.cellar.core.CellarId
import com.drinkit.cellar.core.CellarName
import com.drinkit.cellar.core.CellarRooms
import com.drinkit.common.CityLocation
import com.drinkit.common.Country
import com.drinkit.common.Point
import com.drinkit.user.core.Author
import com.drinkit.user.core.UserId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class DeleteCellarTest {

    private val cellarFixtures = CellarFixtures()
    private val cellars = cellarFixtures.cellars
    private val deleteCellar = cellarFixtures.deleteCellar

    private val owner = UserId("64656d6f0000000000000001")
    private val otherUser = UserId("64656d6f0000000000000002")

    @Test
    fun `deletes a cellar of its owner`() {
        // Given
        val cellarId = givenACellarOf(owner)

        // When
        val result = deleteCellar.invoke(cellarId, Author.Connected(owner))

        // Then
        result shouldBe CellarDeleted
        cellars.findById(cellarId).shouldBeNull()
    }

    @Test
    fun `finds no unknown cellar to delete`() {
        // Given
        val unknown = CellarId("64656d6f00000000000000ff")

        // When
        val result = deleteCellar.invoke(unknown, Author.Connected(owner))

        // Then
        result shouldBe CellarNotFound
    }

    @Test
    fun `finds no cellar of another user to delete, and keeps it`() {
        // Given
        val cellarId = givenACellarOf(owner)

        // When
        val result = deleteCellar.invoke(cellarId, Author.Connected(otherUser))

        // Then
        result shouldBe CellarNotFound
        cellars.findById(cellarId).shouldNotBeNull()
    }

    private fun givenACellarOf(userId: UserId): CellarId {
        val cellar = Cellar(
            id = CellarId("64656d6f0000000000000010"),
            name = CellarName("Cave du sous-sol"),
            location = CityLocation(
                "Lyon",
                Country("FR", "France"),
                Point("45.76".toBigDecimal(), "4.83".toBigDecimal()),
            ),
            rooms = CellarRooms.EMPTY,
            owner = userId,
        )
        return cellars.create(cellar).shouldNotBeNull()
    }
}
