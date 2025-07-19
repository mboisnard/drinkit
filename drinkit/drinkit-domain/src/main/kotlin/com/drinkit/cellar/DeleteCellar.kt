package com.drinkit.cellar

import com.drinkit.user.registration.CompletedUser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteCellar(
    private val cellars: Cellars,
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    operator fun invoke(cellarId: CellarId, connectedUser: CompletedUser) {
        val cellar = cellars.findById(cellarId) ?: error("Cellar not found")

        if (cellar.canBeSeenBy(connectedUser)) {
            logger.info { "Removing cellar $cellarId, author: ${connectedUser.id}" }
            cellars.delete(cellarId)
        } else {
            error("Not authorized to delete cellar $cellarId")
        }
    }
}
