package com.drinkit.domain.cellar

import com.drinkit.domain.user.UserId

interface CellarWriteRepository {
    fun create(cellar: Cellar): CellarId

    fun delete(cellarId: CellarId)
}

interface CellarReadRepository {

    fun findById(cellarId: CellarId): Cellar?

    fun findAllByOwnerId(owner: UserId): Sequence<Cellar>
}