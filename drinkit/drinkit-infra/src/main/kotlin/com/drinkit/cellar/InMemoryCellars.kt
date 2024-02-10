package com.drinkit.cellar

import com.drinkit.user.UserId
import org.springframework.stereotype.Repository

@Repository
internal class InMemoryCellars: ReadCellars, WriteCellars {

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