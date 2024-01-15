package com.drinkit.cellars.api

import com.drinkit.api.generated.api.CellarsApiDelegate
import com.drinkit.api.generated.model.*
import com.drinkit.cellar.*
import com.drinkit.user.ANONYMOUS_USER
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
internal class CellarsApi(
    private val createCellar: CreateCellar,
    private val deleteCellar: DeleteCellar,
    private val findCellars: FindCellars,
): CellarsApiDelegate {

    override fun createCellar(createCellarRequest: CreateCellarRequest): ResponseEntity<CellarId> {
        val command = createCellarRequest.toCommand()

        val createdCellarId = createCellar(command)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createdCellarId)
    }

    override fun deleteCellar(cellarId: CellarId): ResponseEntity<Unit> {
        deleteCellar(cellarId, ANONYMOUS_USER)
        return ResponseEntity.noContent().build()
    }

    override fun findCellars(): ResponseEntity<CellarsResponse> {
        val cellars = findCellars.byOwnerId(ANONYMOUS_USER.id)
            .map { it.toResponse() }
            .toList()

        return ResponseEntity.ok(CellarsResponse(cellars))
    }

    private fun CreateCellarRequest.toCommand() =
        CreateCellarCommand(
            name = CellarName(name),
            location = location.toDomain(),
            owner = ANONYMOUS_USER,
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
                latitude = point!!.latitude,
                longitude = point!!.longitude
            )
        )

    private fun Cellar.toResponse() =
        CellarResponse(
            id = id,
            name = name.value,
            location = location.toResponse(),
        )
}