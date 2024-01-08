package com.drinkit.domain.cellar

import com.drinkit.domain.user.UserId

interface CellarRepository {

    fun create(cellar: Cellar)

    fun findAllByOwnerId(owner: UserId): Sequence<Cellar>
}