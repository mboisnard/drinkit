package com.drinkit.rate.limit

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.reflect.KClass

/** Counts each call of the method as its [policy] says, and throws [RateLimitExceededException] past its rule. */
@TechStarterTool
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimited(val policy: KClass<out RateLimitPolicy>)

/** A bean that holds the rule of a `@RateLimited` method and tells which key each of its calls counts against. */
@TechStarterTool
interface RateLimitPolicy {

    val rule: RateLimitRule

    /** The key the call counts against, such as an email, or null to let it through uncounted. */
    fun key(call: RateLimitedCall): String?
}

@TechStarterTool
class RateLimitedCall(val arguments: List<Any?>) {

    /** The one argument of type [T], such as the request of an endpoint. */
    inline fun <reified T> argument(): T = arguments.filterIsInstance<T>().single()
}
