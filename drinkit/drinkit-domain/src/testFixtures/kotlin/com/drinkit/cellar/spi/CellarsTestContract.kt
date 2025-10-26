package com.drinkit.cellar.spi

abstract class CellarsTestContract {

    private val repository: Cellars by lazy {
        fetchRepository()
    }

    abstract fun fetchRepository(): Cellars
}