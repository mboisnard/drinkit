package com.drinkit.cellars

import com.drinkit.api.generated.api.CellarsApiDelegate
import com.drinkit.api.generated.model.*
import com.drinkit.cellar.*
import com.drinkit.config.ConnectedUser
import com.drinkit.config.ConnectedUserException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
internal class CellarsApi(
    private val createCellar: CreateCellar,
    private val deleteCellar: DeleteCellar,
    private val findCellars: FindCellars,
    private val connectedUser: ConnectedUser?,
): CellarsApiDelegate {

    fun connectedUser() = connectedUser?.getOrFail() ?: throw ConnectedUserException("No connected user bean")

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
        val cellars = findCellars.byOwnerId(connectedUser().id)
            .map { it.toResponse() }
            .toList()

        return ResponseEntity.ok(CellarsResponse(cellars))
    }

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