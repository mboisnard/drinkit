package com.drinkit.cellar

import com.drinkit.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class FindCellars(
    private val cellarRepository: CellarReadRepository,
) {
    private val logger = KotlinLogging.logger { }

    fun byOwnerId(owner: UserId): Sequence<Cellar> {
        logger.debug { "Finding cellars for userId $owner" }
        return cellarRepository.findAllByOwnerId(owner)
    }
}
