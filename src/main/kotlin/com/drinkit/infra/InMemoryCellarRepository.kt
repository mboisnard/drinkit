package com.drinkit.infra

import com.drinkit.domain.cellar.Cellar
import com.drinkit.domain.cellar.CellarId
import com.drinkit.domain.cellar.CellarReadRepository
import com.drinkit.domain.cellar.CellarWriteRepository
import com.drinkit.domain.user.UserId
import org.springframework.stereotype.Repository

@Repository
internal class InMemoryCellarRepository: CellarReadRepository, CellarWriteRepository {

    private var cellars: MutableMap<CellarId, Cellar> = mutableMapOf()
    override fun create(cellar: Cellar): CellarId {
        if (!cellars.containsKey(cellar.id)) {
            cellars[cellar.id] = cellar
            return cellar.id
        }

        throw IllegalStateException("Cellar already exists")
    }

    override fun delete(cellarId: CellarId) {
        cellars.remove(cellarId)
    }

    override fun findById(cellarId: CellarId): Cellar? = cellars[cellarId]

    override fun findAllByOwnerId(owner: UserId): Sequence<Cellar> =
        cellars.filter { it.value.owner == owner }.values.asSequence()
}