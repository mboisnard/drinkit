package com.drinkit.messaging.springevents

import com.drinkit.messaging.PlatformEventHandler
import org.springframework.aop.framework.autoproxy.AutoProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ApplicationListenerMethodAdapter
import org.springframework.context.event.EventListenerFactory
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

@Component
internal class EventHandlerPostProcessor : ApplicationContextAware, SmartInitializingSingleton, BeanFactoryPostProcessor {

    private lateinit var applicationContext: ConfigurableApplicationContext
    private lateinit var beanFactory: ConfigurableListableBeanFactory
    private lateinit var sortedFactories: List<EventListenerFactory>

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        require(applicationContext is ConfigurableApplicationContext) {
            "ApplicationContext does not implement ConfigurableApplicationContext"
        }
        this.applicationContext = applicationContext
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val factories = beanFactory.getBeansOfType(EventListenerFactory::class.java, false, false)
            .values

        this.beanFactory = beanFactory
        this.sortedFactories = factories.sortedWith(AnnotationAwareOrderComparator.INSTANCE)
    }

    override fun afterSingletonsInstantiated() {
        val beanNames = beanFactory.getBeanNamesForType(Any::class.java)

        val evaluatorClass = Class.forName("org.springframework.context.event.EventExpressionEvaluator")
        val evaluatorConstructor = evaluatorClass.declaredConstructors[0]
        evaluatorConstructor.setAccessible(true)
        val evaluator = evaluatorConstructor.newInstance(StandardEvaluationContext())

        beanNames
            .mapNotNull {
                val targetClass = AutoProxyUtils.determineTargetClass(beanFactory, it) ?: return@mapNotNull null
                it to targetClass
            }
            .filter { (_, targetClass) ->
                AnnotationUtils.isCandidateClass(targetClass, PlatformEventHandler::class.java) && !isSpringContainerClass(
                    targetClass
                )
            }
            .forEach { (beanName, targetClass) ->
                val annotatedMethods = try {
                    MethodIntrospector.selectMethods(
                        targetClass,
                        MethodIntrospector.MetadataLookup {
                            AnnotatedElementUtils.findMergedAnnotation(it, PlatformEventHandler::class.java)
                        }
                    )
                } catch (ex: Throwable) {
                    emptyMap()
                }

                annotatedMethods.keys.forEach { method ->
                    sortedFactories.forEach { factory ->
                        if (factory.supportsMethod(method)) {
                            val methodToUse =
                                AopUtils.selectInvocableMethod(method, applicationContext.getType(beanName))
                            val applicationListener =
                                factory.createApplicationListener(beanName, targetClass, methodToUse)

                            if (applicationListener is ApplicationListenerMethodAdapter) {
                                val applicationContextField =
                                    applicationListener.javaClass.getDeclaredField("applicationContext")
                                applicationContextField.setAccessible(true)
                                applicationContextField.set(applicationListener, applicationContext)

                                val evaluatorField = applicationListener.javaClass.getDeclaredField("evaluator")
                                evaluatorField.setAccessible(true)
                                evaluatorField.set(applicationListener, evaluator)
                            }
                            applicationContext.addApplicationListener(applicationListener)
                        }
                    }
                }
            }
    }

    private fun isSpringContainerClass(clazz: Class<*>): Boolean {
        return clazz.name.startsWith("org.springframework.") &&
            !AnnotatedElementUtils.isAnnotated(ClassUtils.getUserClass(clazz), Component::class.java)
    }
}
