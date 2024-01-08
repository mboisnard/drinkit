package com.drinkit.domain.cellar

import com.drinkit.domain.common.CityLocation
import com.drinkit.domain.user.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

data class CreateCellarCommand(
    val name: String,
    val location: CityLocation,
    val owner: User,
)

//@Service
class CreateCellar(
    private val cellarRepository: CellarRepository,
) {
    private val logger = KotlinLogging.logger { }

    operator fun invoke(command: CreateCellarCommand): CellarId {
        val cellarId = CellarId.create()

        logger.debug { "Creating cellar with id $cellarId from command $command" }

        val cellar = with(command) {
            Cellar(
                id = cellarId,
                name = name,
                location = location,
                owner = owner.id,
            )
        }

        cellarRepository.create(cellar)

        return cellarId
    }
}