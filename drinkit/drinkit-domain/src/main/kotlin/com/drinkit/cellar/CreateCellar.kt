package com.drinkit.cellar

import com.drinkit.cellar.core.Cellar
import com.drinkit.cellar.core.CellarId
import com.drinkit.cellar.core.CellarName
import com.drinkit.cellar.core.CellarRooms
import com.drinkit.cellar.spi.Cellars
import com.drinkit.common.CityLocation
import com.drinkit.common.GenerateId
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateCellarCommand(
    val name: CellarName,
    val location: CityLocation,
    val ownerId: UserId,
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
                owner = ownerId,
            )
        }

        return cellars.create(cellar)
    }
}
