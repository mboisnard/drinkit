package com.drinkit.cellar

import com.drinkit.common.CityLocation
import com.drinkit.common.Country
import com.drinkit.common.Point
import com.drinkit.generated.jooq.tables.records.CellarRecord
import com.drinkit.generated.jooq.tables.references.CELLAR
import com.drinkit.jooq.allFields
import com.drinkit.jooq.fetchSequence
import com.drinkit.user.UserId
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDateTime

@Repository
internal class JooqCellars(
    private val dslContext: DSLContext,
    private val clock: Clock,
) : Cellars {

    override fun create(cellar: Cellar): CellarId? {
        val query = dslContext.insertInto(CELLAR)
            .set(CELLAR.ID, cellar.id.value)
            .set(CELLAR.NAME, cellar.name.value)
            .set(CELLAR.LOCATION_CITY, cellar.location.city)
            .set(CELLAR.LOCATION_COUNTRY_CODE, cellar.location.country.code)
            .set(CELLAR.LOCATION_COUNTRY, cellar.location.country.name)
            .set(CELLAR.LOCATION_LATITUDE, cellar.location.point.latitude.toDouble())
            .set(CELLAR.LOCATION_LONGITUDE, cellar.location.point.longitude.toDouble())
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
        val query = dslContext.select(
            allFields(CELLAR)
        )
            .from(CELLAR)
            .where(
                CELLAR.ID.eq(cellarId.value)
            )

        return query.fetchOne()?.value1()?.toCellar()
    }

    override fun findAllByOwnerId(owner: UserId): Sequence<Cellar> {
        val query = dslContext.select(
            allFields(CELLAR)
        )
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
            location = CityLocation(
                city = locationCity,
                country = Country(
                    code = locationCountryCode,
                    name = locationCountry,
                ),
                point = Point(
                    latitude = locationLatitude.toBigDecimal(),
                    longitude = locationLongitude.toBigDecimal(),
                )
            ),
            owner = UserId(ownerId),
        )
}