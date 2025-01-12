package com.drinkit.jooq

import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class JooqConfig {

    @Bean
    fun jooqConfigurationCustomizer(): DefaultConfigurationCustomizer =
            DefaultConfigurationCustomizer {
                it
                        // Override Dialect here to be sure this tech-starter will work with postgresql and not with a dialect determined by a property
                        .set(SQLDialect.POSTGRES)
                        .set(
                                Settings()
                                        .withExecuteWithOptimisticLocking(true)
                                        .withExecuteWithOptimisticLockingExcludeUnversioned(true)
                        )
            }
}
