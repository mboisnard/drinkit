package com.drinkit.cellar

import com.drinkit.cellar.core.Cellar
import com.drinkit.cellar.spi.Cellars
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.user.core.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
@Usecase
class FindCellars(private val cellars: Cellars) {
    private val logger = KotlinLogging.logger { }

    fun byOwnerId(owner: UserId): Sequence<Cellar> {
        logger.debug { "Finding cellars for userId $owner" }
        return cellars.findAllByOwnerId(owner)
    }
}
