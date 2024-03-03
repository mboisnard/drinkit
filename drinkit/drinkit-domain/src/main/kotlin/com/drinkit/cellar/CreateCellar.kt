package com.drinkit.cellar

import com.drinkit.common.CityLocation
import com.drinkit.common.IdGenerator
import com.drinkit.user.CompletedUser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

data class CreateCellarCommand(
    val name: CellarName,
    val location: CityLocation,
    val owner: CompletedUser,
)

@Service
class CreateCellar(
    private val cellars: WriteCellars,
    private val idGenerator: IdGenerator,
) {
    private val logger = KotlinLogging.logger { }

    operator fun invoke(command: CreateCellarCommand): CellarId? {
        val cellarId = CellarId.create(idGenerator)

        logger.debug { "Creating cellar with id $cellarId from command $command" }

        val cellar = with(command) {
            Cellar(
                id = cellarId,
                name = name,
                location = location,
                owner = owner.id,
            )
        }

        return cellars.create(cellar)
    }
}