package com.drinkit.messaging.infra.springevents

import com.drinkit.idempotency.Idempotency
import com.drinkit.messaging.PlatformEvent
import com.drinkit.messaging.PlatformEventHandler
import com.drinkit.messaging.validate
import org.springframework.aop.framework.autoproxy.AutoProxyUtils
import org.springframework.aop.scope.ScopedProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler
import org.springframework.util.backoff.BackOff
import java.lang.reflect.Method
import java.time.Duration

// Runs once every bean exists, as Spring's own @EventListener processor: a handler's bean may depend on the publisher
internal class PlatformEventHandlerProcessor(
    private val context: ConfigurableApplicationContext,
    private val backOff: BackOff,
) : SmartInitializingSingleton,
    DisposableBean {

    // Not a bean: a TaskScheduler bean would replace the one Spring Boot gives the application's @Scheduled methods
    private val scheduler = SimpleAsyncTaskScheduler().apply {
        setVirtualThreads(true)
        setThreadNamePrefix("platform-event-")
        setTaskTerminationTimeout(SHUTDOWN_WAIT.toMillis())
    }

    override fun afterSingletonsInstantiated() {
        val listeners = context.beanFactory.getBeanNamesForType(Any::class.java)
            .filterNot { ScopedProxyUtils.isScopedTarget(it) }
            .flatMap { listenersOf(it) }

        listeners.groupBy { it.handler.name }
            .filterValues { it.size > 1 }
            .keys.firstOrNull()
            ?.let { name ->
                // A broker would make them share one queue, each getting only part of the events
                throw IllegalArgumentException("Two handlers are named $name")
            }

        listeners.forEach { context.addApplicationListener(it) }
    }

    private fun listenersOf(beanName: String): List<PlatformEventHandlerListener> {
        val beanClass = AutoProxyUtils.determineTargetClass(context.beanFactory, beanName)
        if (beanClass == null || !AnnotationUtils.isCandidateClass(beanClass, PlatformEventHandler::class.java)) {
            return emptyList()
        }

        return MethodIntrospector.selectMethods(
            beanClass,
            MethodIntrospector.MetadataLookup {
                AnnotatedElementUtils.findMergedAnnotation(it, PlatformEventHandler::class.java)
            },
        ).map { (method, handler) -> listenerOf(beanName, method, handler) }
    }

    private fun listenerOf(
        beanName: String,
        method: Method,
        handler: PlatformEventHandler,
    ): PlatformEventHandlerListener {
        val eventType = method.parameterTypes.singleOrNull()
        require(eventType != null && PlatformEvent::class.java.isAssignableFrom(eventType)) {
            "Handler ${handler.name}: its only parameter must be a platform event"
        }

        handler.validate()

        val idempotency = if (handler.idempotent) {
            checkNotNull(context.getBeanProvider(Idempotency::class.java).ifAvailable) {
                "Handler ${handler.name} is idempotent: it needs the Idempotency of idempotency-starter"
            }
        } else {
            null
        }

        // The method of the bean the context hands out, so that a proxy around the handler still applies
        val invocable = AopUtils.selectInvocableMethod(method, context.getType(beanName))

        return PlatformEventHandlerListener(
            handler,
            eventType,
            backOff = backOff,
            idempotency = idempotency,
            callHandler = { event ->
                AopUtils.invokeJoinpointUsingReflection(context.getBean(beanName), invocable, arrayOf(event))
            },
            schedule = { delay, task -> scheduler.schedule(task, scheduler.clock.instant().plus(delay)) },
        )
    }

    // A running handler ends before the application stops; a delivery still waiting is lost, as with no broker
    override fun destroy() = scheduler.close()

    private companion object {
        val SHUTDOWN_WAIT: Duration = Duration.ofSeconds(30)
    }
}
