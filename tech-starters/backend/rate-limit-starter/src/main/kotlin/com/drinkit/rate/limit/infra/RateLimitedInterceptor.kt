package com.drinkit.rate.limit.infra

import com.drinkit.rate.limit.RateLimited
import com.drinkit.rate.limit.RateLimitedCall
import com.drinkit.rate.limit.RateLimiter
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.core.annotation.AnnotatedElementUtils

// The beans are read at each call: the advisor is built before the beans it would otherwise freeze unproxied
internal class RateLimitedInterceptor(private val beans: BeanFactory) : MethodInterceptor {

    override fun invoke(invocation: MethodInvocation): Any? {
        val method = AopUtils.getMostSpecificMethod(invocation.method, invocation.`this`?.javaClass)
        val rateLimited = AnnotatedElementUtils.findMergedAnnotation(method, RateLimited::class.java)

        if (rateLimited != null) {
            val policy = beans.getBean(rateLimited.policy.java)
            val key = policy.key(RateLimitedCall(invocation.arguments.toList()))

            if (key != null) {
                beans.getBean<RateLimiter>().consume(policy.rule, key).orThrow()
            }
        }

        return invocation.proceed()
    }
}
