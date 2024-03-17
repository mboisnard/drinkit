package com.drinkit.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
internal class ClockConfiguration {

    @Bean
    fun clock(): Clock {
        return Clock.systemDefaultZone()
    }
}
