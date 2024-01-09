package com.drinkit.domain.cellar

import com.drinkit.domain.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging


data class FindCellars(
    private val cellarRepository: CellarReadRepository,
) {
    private val logger = KotlinLogging.logger { }

    fun byOwnerId(owner: UserId): Sequence<Cellar> {
        logger.debug { "Finding cellars for userId $owner" }
        return cellarRepository.findAllByOwnerId(owner)
    }
}
