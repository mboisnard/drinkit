package com.drinkit.postgresql.infra

import org.jooq.exception.DataChangedException
import org.springframework.dao.DataAccessException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.dao.support.PersistenceExceptionTranslator

// jOOQ throws DataChangedException itself, after the statement: the SQL exception translator never sees it
internal class OptimisticLockTranslator : PersistenceExceptionTranslator {

    override fun translateExceptionIfPossible(ex: RuntimeException): DataAccessException? =
        (ex as? DataChangedException)?.let { OptimisticLockingFailureException(it.message.orEmpty(), it) }
}
