package com.drinkit.cellar

import com.drinkit.common.CityLocation
import com.drinkit.common.GenerateId
import com.drinkit.user.core.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateCellarCommand(
    val name: CellarName,
    val location: CityLocation,
    val owner: User,
)

@Service
class CreateCellar(
    private val cellars: Cellars,
    private val generateId: GenerateId,
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    operator fun invoke(command: CreateCellarCommand): CellarId? {
        val cellarId = generateId.invoke(CellarId::class)
        logger.debug { "Creating cellar with id $cellarId from command $command" }

        val cellar = with(command) {
            Cellar(
                id = cellarId,
                name = name,
                location = location,
                rooms = CellarRooms.EMPTY,
                owner = owner.id,
            )
        }

        return cellars.create(cellar)
    }
}
