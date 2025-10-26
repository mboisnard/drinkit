package com.drinkit.cellar

import com.drinkit.cellar.spi.InMemoryCellars
import com.drinkit.common.MockGenerateId
import com.drinkit.messaging.SpyPlatformEventPublisher
import com.drinkit.test.ControlledClock

class CellarFixtures(
    val generateId: MockGenerateId = MockGenerateId(),
) {
    val spyEventPublisher = SpyPlatformEventPublisher()

    val controlledClock = ControlledClock()

    val cellars = InMemoryCellars()

    val createCellar = CreateCellar(
        cellars = cellars,
        generateId = generateId,
    )

    val deleteCellar = DeleteCellar(
        cellars = cellars,
    )

    val findCellars = FindCellars(
        cellars = cellars,
    )
}