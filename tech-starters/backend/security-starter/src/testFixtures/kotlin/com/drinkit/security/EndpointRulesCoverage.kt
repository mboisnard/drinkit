package com.drinkit.security

import com.drinkit.security.infra.chain.SecurityAutoConfiguration
import com.drinkit.security.infra.chain.UnruledEndpoint
import com.drinkit.security.infra.code.EmailCodeAuthenticationProvider
import com.drinkit.security.infra.code.SignInCode
import com.drinkit.security.infra.code.SignInCodes
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory
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
 * and asks the starter's chain which ones it refuses. The chain holds [rules], by the names of their beans, plus the
 * rules of the other tech starters on the classpath, such as the stream of sse-starter, under the application's
 * [properties]: a rule named as a starter's replaces it, and a starter disabled brings none. No application is started.
 */
class EndpointRulesCoverage(
    private val rules: Map<String, EndpointRules>,
    private val apiPackage: String,
    private val properties: List<String> = emptyList(),
) {

    constructor(rules: List<EndpointRules>, apiPackage: String, properties: List<String> = emptyList()) :
        this(rules.withIndex().associate { (index, rule) -> "endpointRules$index" to rule }, apiPackage, properties)

    private val anonymous = AnonymousAuthenticationToken(
        "coverage",
        "anonymous",
        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
    )

    fun unruledEndpoints(): List<String> {
        val endpoints = endpoints()
        val unruled = mutableListOf<Pair<String, String>>()
        val chain = properties.fold(securityChain()) { runner, property -> runner.withPropertyValues(property) }
        rules.entries.fold(chain) { runner, (name, rule) ->
            runner.withBean(name, EndpointRules::class.java, { rule })
        }.run { context ->
            endpoints.filterTo(unruled) { (method, path) -> isUnruled(context, method, path) }
        }
        return unruled.map { (method, path) -> "$method $path" }.sorted()
    }

    private fun securityChain() = startersRules().fold(
        WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration::class.java))
            .withBean(JsonMapper::class.java, { JsonMapper() })
            .withBean(EmailCodeAuthenticationProvider::class.java, {
                EmailCodeAuthenticationProvider(NoSignInCodes, InMemoryUserAccounts(), Clock.systemUTC())
            }),
    ) { runner, starter -> runner.withConfiguration(AutoConfigurations.of(starter)) }

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

    // The auto-configurations that bring their rules when security-starter is there, read without loading any class
    private fun startersRules(): List<Class<*>> {
        val classLoader = javaClass.classLoader
        val metadata = SimpleMetadataReaderFactory(classLoader)

        return ImportCandidates.load(AutoConfiguration::class.java, classLoader)
            .filter { name ->
                val condition = metadata.getMetadataReader(name).annotationMetadata
                    .getAnnotationAttributes(ConditionalOnClass::class.java.name, true)
                (condition?.get("value") as Array<*>?).orEmpty().contains(EndpointRules::class.java.name)
            }
            .map { ClassUtils.forName(it, classLoader) }
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
