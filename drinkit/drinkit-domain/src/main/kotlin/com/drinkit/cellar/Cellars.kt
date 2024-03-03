package com.drinkit.cellar

import com.drinkit.user.UserId

interface Cellars {
    fun create(cellar: Cellar): CellarId?

    fun delete(cellarId: CellarId): Int

    fun findById(cellarId: CellarId): Cellar?

    fun findAllByOwnerId(owner: UserId): Sequence<Cellar>
}