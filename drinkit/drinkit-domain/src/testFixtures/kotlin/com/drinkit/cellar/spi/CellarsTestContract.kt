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
        val ownCellars = listOf(cellarOf(OWNER, id = 1), cellarOf(OWNER, id = 2))
        (ownCellars + cellarOf(NEIGHBOUR, id = 3)).forEach { repository.create(it) }

        repository.findAllByOwnerId(OWNER).toList() shouldContainExactlyInAnyOrder ownCellars
    }

    private fun cellarOf(owner: UserId, id: Int) = Cellar(
        id = CellarId(hexId(id)),
        name = CellarName("Cave $id"),
        location = CityLocation(
            "Bordeaux",
            Country("FR", "France"),
            Point("44.84".toBigDecimal(), "-0.58".toBigDecimal()),
        ),
        rooms = CellarRooms.EMPTY,
        owner = owner,
    )

    private companion object {
        val OWNER = UserId(hexId(101))
        val NEIGHBOUR = UserId(hexId(102))

        fun hexId(value: Int) = value.toString(radix = 16).padStart(length = 24, padChar = '0')
    }
}
