package com.drinkit.messaging.springevents

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.support.TaskUtils


@EnableAsync
@Configuration
internal class SpringEventsConfig {

    @Bean
    fun applicationEventMulticaster(): ApplicationEventMulticaster {
        val eventMulticaster = SimpleApplicationEventMulticaster()
        eventMulticaster.setTaskExecutor(SimpleAsyncTaskExecutor())
        eventMulticaster.setErrorHandler(TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER)
        return eventMulticaster
    }
}