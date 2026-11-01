package com.drinkit.messaging.infra.springevents

import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus

// Runs the transaction synchronizations, commit or rollback, with no resource behind them
internal class NoOpTransactionManager : AbstractPlatformTransactionManager() {

    override fun doGetTransaction(): Any = Any()

    override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit

    override fun doCommit(status: DefaultTransactionStatus) = Unit

    override fun doRollback(status: DefaultTransactionStatus) = Unit
}
