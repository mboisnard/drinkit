package com.drinkit.cellar

import com.drinkit.user.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class DeleteCellar(
    private val cellarWriteRepository: CellarWriteRepository,
    private val cellarReadRepository: CellarReadRepository,
) {
    private val logger = KotlinLogging.logger { }

    operator fun invoke(cellarId: CellarId, connectedUser: User) {
        val cellar = cellarReadRepository.findById(cellarId) ?: throw IllegalStateException("Cellar not found")

        if (cellar.canBeSeenBy(connectedUser)) {
            logger.info { "Removing cellar $cellarId, author: ${connectedUser.id}" }
            cellarWriteRepository.delete(cellarId)
        } else {
            throw IllegalStateException("Not authorized to delete cellar $cellarId")
        }
    }
}