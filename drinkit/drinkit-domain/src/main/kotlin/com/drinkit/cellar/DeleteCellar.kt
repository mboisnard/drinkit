package com.drinkit.cellar

import com.drinkit.cellar.core.CellarId
import com.drinkit.cellar.spi.Cellars
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.user.core.Author
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Usecase
class DeleteCellar(private val cellars: Cellars) {
    sealed interface Result {
        object CellarDeleted : Result

        // A cellar the author cannot see is not found for them, so that its existence stays hidden
        object CellarNotFound : Result
    }

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun invoke(cellarId: CellarId, author: Author.Connected): Result {
        val cellar = cellars.findById(cellarId)
            ?.takeIf { it.canBeSeenBy(author.userId) }
            ?: return Result.CellarNotFound

        logger.info { "Removing cellar ${cellar.id}, author: ${author.userId}" }
        cellars.delete(cellar.id)
        return Result.CellarDeleted
    }
}
