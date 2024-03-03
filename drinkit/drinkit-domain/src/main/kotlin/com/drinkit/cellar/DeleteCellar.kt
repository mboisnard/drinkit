package com.drinkit.cellar

import com.drinkit.user.CompletedUser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class DeleteCellar(
    private val cellars: Cellars,
) {
    private val logger = KotlinLogging.logger { }

    operator fun invoke(cellarId: CellarId, connectedUser: CompletedUser) {
        val cellar = cellars.findById(cellarId) ?: throw IllegalStateException("Cellar not found")

        if (cellar.canBeSeenBy(connectedUser)) {
            logger.info { "Removing cellar $cellarId, author: ${connectedUser.id}" }
            cellars.delete(cellarId)
        } else {
            throw IllegalStateException("Not authorized to delete cellar $cellarId")
        }
    }
}