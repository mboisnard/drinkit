package com.drinkit.cellar

internal class InMemoryCellarsTest : CellarsTestContract() {

    override fun fetchRepository(): Cellars =
        InMemoryCellars()
}
