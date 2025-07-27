package com.drinkit.cellars

import com.drinkit.api.generated.api.CellarsApiDelegate
import com.drinkit.api.generated.model.CellarResponse
import com.drinkit.api.generated.model.CellarsResponse
import com.drinkit.api.generated.model.CityLocation
import com.drinkit.api.generated.model.Country
import com.drinkit.api.generated.model.CreateCellarRequest
import com.drinkit.api.generated.model.Point
import com.drinkit.cellar.Cellar
import com.drinkit.cellar.CellarId
import com.drinkit.cellar.CellarName
import com.drinkit.cellar.CreateCellar
import com.drinkit.cellar.CreateCellarCommand
import com.drinkit.cellar.DeleteCellar
import com.drinkit.cellar.FindCellars
import com.drinkit.config.AbstractApi
import com.drinkit.config.ConnectedUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
@PreAuthorize("isAuthenticated()")
internal class CellarsApi(
    private val createCellar: CreateCellar,
    private val deleteCellar: DeleteCellar,
    private val findCellars: FindCellars,
    private val connectedUser: ConnectedUser,
) : CellarsApiDelegate, AbstractApi() {

    override fun createCellar(createCellarRequest: CreateCellarRequest): ResponseEntity<CellarId> {
        val command = createCellarRequest.toCommand()

        val createdCellarId = createCellar(command)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createdCellarId)
    }

    override fun deleteCellar(cellarId: CellarId): ResponseEntity<Unit> {
        deleteCellar(cellarId, connectedUser())
        return ResponseEntity.noContent().build()
    }

    override fun findCellars(): ResponseEntity<CellarsResponse> {
        val cellars = findCellars.byOwnerId(connectedUserIdOrFail())
            .map { it.toResponse() }
            .toList()

        return ResponseEntity.ok(CellarsResponse(cellars))
    }

    private fun connectedUser() = connectedUser.getOrFail()

    private fun CreateCellarRequest.toCommand() =
        CreateCellarCommand(
            name = CellarName(name),
            location = location.toDomain(),
            owner = connectedUser(),
        )

    private fun CityLocation.toDomain() =
        com.drinkit.common.CityLocation(
            city = city,
            country = com.drinkit.common.Country(
                name = country.name,
                code = country.code,
            ),
            point = com.drinkit.common.Point(
                latitude = point.latitude,
                longitude = point.longitude
            )
        )

    private fun com.drinkit.common.CityLocation.toResponse() =
        CityLocation(
            city = city,
            country = Country(
                name = country.name,
                code = country.code,
            ),
            point = Point(
                latitude = point.latitude,
                longitude = point.longitude
            )
        )

    private fun Cellar.toResponse() =
        CellarResponse(
            id = id,
            name = name.value,
            location = location.toResponse(),
        )
}
