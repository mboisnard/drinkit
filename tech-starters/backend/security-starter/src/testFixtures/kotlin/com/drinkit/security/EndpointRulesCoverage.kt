package com.drinkit.security

import com.drinkit.security.infra.chain.SecurityAutoConfiguration
import com.drinkit.security.infra.chain.UnruledEndpoint
import com.drinkit.security.infra.code.EmailCodeAuthenticationProvider
import com.drinkit.security.infra.code.SignInCode
import com.drinkit.security.infra.code.SignInCodes
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant

private val PATH_VARIABLE = Regex("""\{[^}]*}""")

/**
 * Reads the Spring mappings of the types of [apiPackage], such as the interfaces generated from an OpenAPI contract,
 * and asks the starter's chain, built with [rules] alone, which ones it refuses. No application is started.
 * [servedByStarters] lists the operations another tech starter serves and opens, such as `GET /sse/event-stream`.
 */
class EndpointRulesCoverage(
    private val rules: List<EndpointRules>,
    private val apiPackage: String,
    private val servedByStarters: List<String> = emptyList(),
) {

    private val anonymous = AnonymousAuthenticationToken(
        "coverage",
        "anonymous",
        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
    )

    fun unruledEndpoints(): List<String> {
        val endpoints = endpoints()
        val unruled = mutableListOf<Pair<String, String>>()
        rules.foldIndexed(securityChain()) { index, runner, rule ->
            runner.withBean("endpointRules$index", EndpointRules::class.java, { rule })
        }.run { context ->
            // The matcher of the actuator's health endpoint finds the context through the servlet context
            context.servletContext?.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context)
            endpoints.filterTo(unruled) { (method, path) ->
                "$method $path" !in servedByStarters && isUnruled(context, method, path)
            }
        }
        return unruled.map { (method, path) -> "$method $path" }.sorted()
    }

    private fun securityChain() = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration::class.java))
        .withBean(JsonMapper::class.java, { JsonMapper() })
        .withBean(EmailCodeAuthenticationProvider::class.java, {
            EmailCodeAuthenticationProvider(NoSignInCodes, InMemoryUserAccounts(), Clock.systemUTC())
        })

    private fun isUnruled(context: WebApplicationContext, method: String, path: String): Boolean {
        val request = MockHttpServletRequest(context.servletContext, method, path.replace(PATH_VARIABLE, "sample"))
        val authorization = context.getBean(FilterChainProxy::class.java).filterChains
            .firstOrNull { it.matches(request) }
            ?.filters?.filterIsInstance<AuthorizationFilter>()?.singleOrNull()
        return authorization?.authorizationManager?.authorize({ anonymous }, request) == UnruledEndpoint.Refused
    }

    private fun endpoints(): List<Pair<String, String>> = apiTypes().flatMap { type ->
        val prefixes = findMergedAnnotation(type, RequestMapping::class.java)?.path?.toList().orEmpty()
            .ifEmpty { listOf("") }
        type.methods.toList().flatMap { method ->
            val mapping = findMergedAnnotation(method, RequestMapping::class.java) ?: return@flatMap emptyList()
            val verbs = mapping.method.map { it.name }.ifEmpty { listOf("GET") }
            val paths = prefixes.flatMap { prefix -> mapping.path.ifEmpty { arrayOf("") }.map { join(prefix, it) } }
            verbs.flatMap { verb -> paths.map { verb to it } }
        }
    }.distinct()

    private fun apiTypes(): List<Class<*>> {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition) =
                beanDefinition.metadata.isIndependent
        }
        scanner.addIncludeFilter { _, _ -> true }
        return scanner.findCandidateComponents(apiPackage)
            .map { ClassUtils.forName(checkNotNull(it.beanClassName), javaClass.classLoader) }
    }

    private fun join(prefix: String, path: String) =
        listOf(prefix, path).filter { it.isNotEmpty() }.joinToString("") { "/" + it.trim('/') }.ifEmpty { "/" }
}

// The chain needs a sign-in by code to be built, which no request of the coverage ever uses
private object NoSignInCodes : SignInCodes {
    override fun replace(code: SignInCode, now: Instant) = Unit

    override fun tryCode(login: String, codeHash: String, now: Instant) = false

    override fun deleteExpiredAt(now: Instant) = Unit
}
