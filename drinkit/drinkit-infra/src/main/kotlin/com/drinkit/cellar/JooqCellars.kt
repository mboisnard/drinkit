package com.drinkit.cellar

import com.drinkit.cellar.CellarRooms.CellarRoom
import com.drinkit.common.CityLocation
import com.drinkit.generated.jooq.tables.records.CellarRecord
import com.drinkit.generated.jooq.tables.references.CELLAR
import com.drinkit.jooq.JSONBToJacksonConverter
import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import com.drinkit.user.UserId
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime

@Repository
internal class JooqCellars(
    private val dslContext: DSLContext,
    private val clock: Clock,
    objectMapper: ObjectMapper,
) : Cellars {

    private val cityLocationConverter = JSONBToJacksonConverter(CityLocation::class.java, objectMapper)
    private val cellarRoomsConverter = JSONBToJacksonConverter(Set::class.java, objectMapper)

    override fun create(cellar: Cellar): CellarId? {
        val query = dslContext.insertInto(CELLAR)
            .set(CELLAR.ID, cellar.id.value)
            .set(CELLAR.NAME, cellar.name.value)
            .set(CELLAR.LOCATION, cityLocationConverter.to(cellar.location))
            .set(CELLAR.ROOMS, cellarRoomsConverter.to(cellar.rooms.allAsString()))
            .set(CELLAR.OWNER_ID, cellar.owner.value)
            .set(CELLAR.MODIFIED, LocalDateTime.now(clock))
            .onConflict(CELLAR.ID)
            .doNothing()

        val insertedRowCount = query.execute()

        return if (insertedRowCount != 0) cellar.id else null
    }

    override fun delete(cellarId: CellarId): Int {
        val query = dslContext.deleteFrom(CELLAR)
            .where(CELLAR.ID.eq(cellarId.value))
            .limit(1)

        return query.execute()
    }

    override fun findById(cellarId: CellarId): Cellar? {
        val query = dslContext.select(allFields(CELLAR))
            .from(CELLAR)
            .where(
                CELLAR.ID.eq(cellarId.value)
            )

        return query.fetchOne { it.value1() }?.toCellar()
    }

    override fun findAllByOwnerId(owner: UserId): Sequence<Cellar> {
        val query = dslContext.select(allFields(CELLAR))
            .from(CELLAR)
            .where(
                CELLAR.OWNER_ID.eq(owner.value)
            )

        return query.fetchSequence({ it.value1() })
            .map { it.toCellar() }
    }

    private fun CellarRecord.toCellar(): Cellar =
        Cellar(
            id = CellarId(id),
            name = CellarName(name),
            location = cityLocationConverter.from(location),
            rooms = CellarRooms(cellarRoomsConverter.from(rooms).map { CellarRoom(it as String) }.toSet()),
            owner = UserId(ownerId),
        )
}
