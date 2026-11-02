package com.drinkit.postgresql.infra

import org.jooq.conf.ExecuteWithoutWhere
import org.jooq.conf.Settings

internal fun postgresqlSettings(): Settings = Settings()
    .withExecuteWithOptimisticLocking(true)
    .withExecuteWithOptimisticLockingExcludeUnversioned(true)
    .withExecuteUpdateWithoutWhere(ExecuteWithoutWhere.THROW)
    .withExecuteDeleteWithoutWhere(ExecuteWithoutWhere.THROW)
