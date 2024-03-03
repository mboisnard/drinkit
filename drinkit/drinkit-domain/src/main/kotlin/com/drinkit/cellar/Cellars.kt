package com.drinkit.cellar

import com.drinkit.user.UserId

interface WriteCellars {
    fun create(cellar: Cellar): CellarId?

    fun delete(cellarId: CellarId): Int
}

interface ReadCellars {

    fun findById(cellarId: CellarId): Cellar?

    fun findAllByOwnerId(owner: UserId): Sequence<Cellar>
}