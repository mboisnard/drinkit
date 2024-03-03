package com.drinkit.cellar

import com.drinkit.user.UserId

class InMemoryCellars: ReadCellars, WriteCellars {

    private var cellars: MutableMap<CellarId, Cellar> = mutableMapOf()

    override fun create(cellar: Cellar): CellarId? {
        if (cellars.containsKey(cellar.id)) {
            return null
        }

        cellars[cellar.id] = cellar
        return cellar.id
    }

    override fun delete(cellarId: CellarId): Int {
        return if (cellars.remove(cellarId) != null) 1 else 0
    }

    override fun findById(cellarId: CellarId): Cellar? = cellars[cellarId]

    override fun findAllByOwnerId(owner: UserId): Sequence<Cellar> =
        cellars.filter { it.value.owner == owner }.values.asSequence()
}