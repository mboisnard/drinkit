package com.drinkit.jooq

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource


@Configuration
internal class JooqConfiguration {

    @Bean
    fun dslContext(dataSource: DataSource): DSLContext {
        val provider = DataSourceConnectionProvider(TransactionAwareDataSourceProxy(dataSource))

        val jooqConfiguration = DefaultConfiguration()
            .set(provider)
            .set(SQLDialect.POSTGRES)
            .set(
                Settings()
                    .withExecuteWithOptimisticLocking(true)
                    .withExecuteWithOptimisticLockingExcludeUnversioned(true)
            )

        return DefaultDSLContext(jooqConfiguration)
    }
}