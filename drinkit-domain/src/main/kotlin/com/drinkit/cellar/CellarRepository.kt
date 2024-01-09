package com.drinkit.cellar

import com.drinkit.user.UserId

interface CellarWriteRepository {
    fun create(cellar: Cellar): CellarId

    fun delete(cellarId: CellarId)
}

interface CellarReadRepository {

    fun findById(cellarId: CellarId): Cellar?

    fun findAllByOwnerId(owner: UserId): Sequence<Cellar>
}