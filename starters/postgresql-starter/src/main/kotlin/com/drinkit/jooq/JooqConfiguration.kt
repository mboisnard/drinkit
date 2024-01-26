package com.drinkit.jooq

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource


@Configuration
internal class JooqConfiguration {

    @Bean
    fun dslContext(dataSource: DataSource): DSLContext {
        return DSL.using(
            dataSource.connection, SQLDialect.POSTGRES,
            Settings()
                .withExecuteWithOptimisticLocking(true)
                .withExecuteWithOptimisticLockingExcludeUnversioned(true)
        )
    }
}