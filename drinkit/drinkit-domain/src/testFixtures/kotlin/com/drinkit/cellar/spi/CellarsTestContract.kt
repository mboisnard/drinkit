package com.drinkit.cellar.spi

import com.drinkit.cellar.core.Cellar
import com.drinkit.cellar.core.CellarId
import com.drinkit.cellar.core.CellarName
import com.drinkit.cellar.core.CellarRooms
import com.drinkit.common.CityLocation
import com.drinkit.common.Country
import com.drinkit.common.Point
import com.drinkit.user.core.UserId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

abstract class CellarsTestContract {

    private val repository: Cellars by lazy {
        fetchRepository()
    }

    abstract fun fetchRepository(): Cellars

    @Test
    fun `finds the cellars of their owner`() {
        val ownCellars = listOf(cellarOf(OWNER, number = 1), cellarOf(OWNER, number = 2))
        (ownCellars + cellarOf(NEIGHBOUR, number = 3)).forEach { repository.create(it) }

        repository.findAllByOwnerId(OWNER).toList() shouldContainExactlyInAnyOrder ownCellars
    }

    private fun cellarOf(owner: UserId, number: Int) = Cellar(
        id = CellarId.create(),
        name = CellarName("Cave $number"),
        location = CityLocation(
            "Bordeaux",
            Country("FR", "France"),
            Point("44.84".toBigDecimal(), "-0.58".toBigDecimal()),
        ),
        rooms = CellarRooms.EMPTY,
        owner = owner,
    )

    private companion object {
        val OWNER = UserId.create()
        val NEIGHBOUR = UserId.create()
    }
}
