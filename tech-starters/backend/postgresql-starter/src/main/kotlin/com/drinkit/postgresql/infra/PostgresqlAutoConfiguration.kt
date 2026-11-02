package com.drinkit.postgresql.infra

import org.jooq.ExecuteListenerProvider
import org.jooq.SQLDialect
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.dao.support.PersistenceExceptionTranslator
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.postgresql", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(PostgresqlProperties::class)
internal class PostgresqlAutoConfiguration {

    // Backs off by name: a customizer the application adds for another purpose keeps the starter's settings
    @Bean
    @ConditionalOnMissingBean(name = ["postgresqlJooqCustomizer"])
    fun postgresqlJooqCustomizer(): DefaultConfigurationCustomizer = DefaultConfigurationCustomizer {
        it.set(SQLDialect.POSTGRES).set(postgresqlSettings())
    }

    @Bean
    @ConditionalOnMissingBean
    fun postgresqlCheck(dataSource: ObjectProvider<DataSource>): PostgresqlCheck = PostgresqlCheck {
        dataSource.getObject().connection.use { it.metaData.databaseProductName }
    }

    @Bean
    @ConditionalOnMissingBean(name = ["slowQueryListener"])
    fun slowQueryListener(properties: PostgresqlProperties): ExecuteListenerProvider =
        DefaultExecuteListenerProvider(SlowQueryListener(properties.slowQueryThreshold))

    @Bean
    @ConditionalOnMissingBean(name = ["optimisticLockTranslator"])
    fun optimisticLockTranslator(): PersistenceExceptionTranslator = OptimisticLockTranslator()
}
