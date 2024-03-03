package com.drinkit.cellar

abstract class CellarsTestContract {

    private val repository: Cellars by lazy {
        fetchRepository()
    }

    abstract fun fetchRepository(): Cellars
}