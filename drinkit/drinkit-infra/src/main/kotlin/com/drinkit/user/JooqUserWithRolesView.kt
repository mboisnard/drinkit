package com.drinkit.user

import com.drinkit.generated.jooq.tables.records.RoleRecord
import com.drinkit.generated.jooq.tables.records.UserRecord
import org.jooq.Record
import org.jooq.Record2
import org.jooq.RecordMapper

internal class JooqUserWithRolesView(
    val user: UserRecord,
    val roles: List<RoleRecord>,
) : Record by user

internal val userWithRolesRecordMapper = RecordMapper<Record2<UserRecord, List<RoleRecord>>, JooqUserWithRolesView> {
    JooqUserWithRolesView(
        user = it.value1(),
        roles = it.value2()
    )
}