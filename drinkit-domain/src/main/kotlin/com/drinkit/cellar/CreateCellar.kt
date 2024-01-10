package com.drinkit.cellar

import com.drinkit.common.CityLocation
import com.drinkit.user.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

data class CreateCellarCommand(
    val name: CellarName,
    val location: CityLocation,
    val owner: User,
)

@Service
class CreateCellar(
    private val cellarRepository: CellarWriteRepository,
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

        return cellarRepository.create(cellar)
    }
}