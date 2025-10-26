package com.drinkit.cellar.spi

internal class InMemoryCellarsTest : CellarsTestContract() {

    override fun fetchRepository(): Cellars =
            InMemoryCellars()
}