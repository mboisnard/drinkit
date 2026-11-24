package com.drinkit.cellar

import com.drinkit.cellar.spi.InMemoryCellars
import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.test.ControlledClock

class CellarFixtures {
    val spyEventPublisher = SpyPlatformEventPublisher()

    val controlledClock = ControlledClock()

    val cellars = InMemoryCellars()

    val createCellar = CreateCellar(
        cellars = cellars,
    )

    val deleteCellar = DeleteCellar(
        cellars = cellars,
    )

    val findCellars = FindCellars(
        cellars = cellars,
    )
}
