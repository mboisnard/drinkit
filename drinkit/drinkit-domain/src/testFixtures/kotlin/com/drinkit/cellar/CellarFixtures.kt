package com.drinkit.cellar

import com.drinkit.common.ControlledClock
import com.drinkit.common.ControlledIdGenerator
import com.drinkit.messaging.SpyPlatformEventPublisher

class CellarFixtures {

    val controlledIdGenerator = ControlledIdGenerator()

    val spyEventPublisher = SpyPlatformEventPublisher()

    val controlledClock = ControlledClock()

    val cellars = InMemoryCellars()

    val createCellar = CreateCellar(
        cellars = cellars,
        idGenerator = controlledIdGenerator,
    )

    val deleteCellar = DeleteCellar(
        cellars = cellars,
    )

    val findCellars = FindCellars(
        cellars = cellars,
    )
}