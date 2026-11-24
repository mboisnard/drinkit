package com.drinkit.spring.infra

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import java.time.Clock

@AutoConfiguration
@ConditionalOnBooleanProperty("starters.spring.enabled", matchIfMissing = true)
internal class ClockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun clock(): Clock = Clock.systemDefaultZone()
}
