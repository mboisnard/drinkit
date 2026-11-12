package com.drinkit.cellar

import com.drinkit.api.generated.api.CellarsApi
import com.drinkit.api.generated.model.CellarResponse
import com.drinkit.api.generated.model.CellarsResponse
import com.drinkit.api.generated.model.CityLocation
import com.drinkit.api.generated.model.Country
import com.drinkit.api.generated.model.CreateCellarRequest
import com.drinkit.api.generated.model.Point
import com.drinkit.cellar.core.Cellar
import com.drinkit.cellar.core.CellarId
import com.drinkit.cellar.core.CellarName
import com.drinkit.idempotency.Idempotent
import com.drinkit.security.CurrentUser
import com.drinkit.user.author
import com.drinkit.user.userId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@PreAuthorize("isAuthenticated()")
internal class CellarsController(
    private val createCellar: CreateCellar,
    private val deleteCellar: DeleteCellar,
    private val findCellars: FindCellars,
    private val currentUser: CurrentUser,
) : CellarsApi {

    @Idempotent
    override fun createCellar(
        createCellarRequest: CreateCellarRequest,
        idempotencyKey: String?,
    ): ResponseEntity<CellarId> {
        val command = createCellarRequest.toCommand()

        val createdCellarId = createCellar.invoke(command)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createdCellarId)
    }

    @Idempotent
    override fun deleteCellar(cellarId: CellarId, idempotencyKey: String?): ResponseEntity<Unit> =
        when (deleteCellar.invoke(cellarId, currentUser.get().author)) {
            DeleteCellar.Result.CellarDeleted -> ResponseEntity.noContent().build()
            DeleteCellar.Result.CellarNotFound -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

    @Transactional(readOnly = true)
    override fun findCellars(): ResponseEntity<CellarsResponse> {
        val cellars = findCellars.byOwnerId(currentUser.get().userId)
            .map { it.toResponse() }
            .toList()

        return ResponseEntity.ok(CellarsResponse(cellars))
    }

    private fun CreateCellarRequest.toCommand() = CreateCellarCommand(
        name = CellarName(name),
        location = location.toDomain(),
        ownerId = currentUser.get().userId,
    )

    private fun CityLocation.toDomain() = com.drinkit.common.CityLocation(
        city = city,
        country = com.drinkit.common.Country(
            name = country.name,
            code = country.code,
        ),
        point = com.drinkit.common.Point(
            latitude = point.latitude,
            longitude = point.longitude,
        ),
    )

    private fun com.drinkit.common.CityLocation.toResponse() = CityLocation(
        city = city,
        country = Country(
            name = country.name,
            code = country.code,
        ),
        point = Point(
            latitude = point.latitude,
            longitude = point.longitude,
        ),
    )

    private fun Cellar.toResponse() = CellarResponse(
        id = id,
        name = name.value,
        location = location.toResponse(),
    )
}
