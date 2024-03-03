package com.drinkit.cellar

import com.drinkit.common.ControlledClock
import com.drinkit.common.ControlledIdGenerator
import com.drinkit.messaging.SpyEventPublisher

class CellarFixtures {

    val controlledIdGenerator = ControlledIdGenerator()

    val spyEventPublisher = SpyEventPublisher()

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