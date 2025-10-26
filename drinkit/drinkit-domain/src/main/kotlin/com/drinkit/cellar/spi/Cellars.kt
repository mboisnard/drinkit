package com.drinkit.cellar.spi

import com.drinkit.cellar.core.Cellar
import com.drinkit.cellar.core.CellarId
import com.drinkit.user.core.UserId

interface Cellars {
    fun create(cellar: Cellar): CellarId?

    fun delete(cellarId: CellarId): Int

    fun findById(cellarId: CellarId): Cellar?

    fun findAllByOwnerId(owner: UserId): Sequence<Cellar>
}