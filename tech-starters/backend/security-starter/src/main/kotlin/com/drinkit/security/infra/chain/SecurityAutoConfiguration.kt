package com.drinkit.security.infra.chain

import com.drinkit.security.EndpointRules
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.SecurityStarterProperties.Cookie.Companion.SAME_SITE
import com.drinkit.security.infra.code.CODE_REQUEST_PATH
import com.drinkit.security.infra.code.CODE_SIGN_IN_PATH
import com.drinkit.security.infra.code.EmailCodeAuthenticationProvider
import com.drinkit.security.infra.code.EmailCodeSignInConfigurer
import com.drinkit.security.infra.code.EmailCodeSignInFilter
import com.drinkit.security.infra.events.SecurityEventLogger
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authorization.AuthorizationEventPublisher
import org.springframework.security.authorization.SpringAuthorizationEventPublisher
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.DelegatingSecurityContextRepository
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.security.web.savedrequest.NullRequestCache
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.json.JsonMapper

private const val LOGOUT_PATH = "/api/auth/logout"
private const val CSRF_PATH = "/api/auth/csrf"

private const val MANAGEMENT_WEB_SECURITY =
    "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration"
private const val ACTUATOR_ENDPOINTS =
    "org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration"

/**
 * Builds the one filter chain that protects every request of the application: the sign-in by a code sent by email,
 * the logout, the CSRF protection of a single-page app, the security headers, and the access rules the application
 * declares as [EndpointRules]. It refuses any request no rule matches, so that a new endpoint is closed until someone
 * opens it, and it registers before Spring Boot's own chains, which then back off.
 *
 * OWASP: ASVS 5.0 V8.2.1, V3.4, Authorization and HTTP Headers Cheat Sheets (deny by default, security headers).
 */
@AutoConfiguration(
    before = [ServletWebSecurityAutoConfiguration::class],
    beforeName = [MANAGEMENT_WEB_SECURITY],
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
@EnableWebSecurity
@EnableMethodSecurity
internal class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Suppress("LongParameterList")
    fun securityFilterChain(
        http: HttpSecurity,
        context: ApplicationContext,
        endpointRules: ObjectProvider<EndpointRules>,
        rememberMeServices: ObjectProvider<RememberMeServices>,
        emailCodeAuthenticationProvider: EmailCodeAuthenticationProvider,
        csrfTokenRepository: CsrfTokenRepository,
        sessionAuthenticationStrategy: SessionAuthenticationStrategy,
        securityContextRepository: SecurityContextRepository,
        jsonMapper: JsonMapper,
        properties: SecurityStarterProperties,
    ): SecurityFilterChain {
        val errorResponses = SecurityErrorResponses(jsonMapper)

        // Outside the Kotlin DSL, which has no spa() yet (spring-security#18332). spa() accepts the token a
        // single-page app reads from the XSRF cookie and sends back in the X-XSRF-TOKEN header.
        http.csrf { it.spa().csrfTokenRepository(csrfTokenRepository) }
        rememberMeServices.ifAvailable { services -> http.rememberMe { it.rememberMeServices(services) } }
        http.authenticationProvider(emailCodeAuthenticationProvider)
        // Spring Boot also gives the provider bean to a parent manager, which would try a refused code a second time
        http.getSharedObject(AuthenticationManagerBuilder::class.java).parentAuthenticationManager(null)

        http {
            securityContext { this.securityContextRepository = securityContextRepository }
            sessionManagement { this.sessionAuthenticationStrategy = sessionAuthenticationStrategy }
            exceptionHandling {
                authenticationEntryPoint = errorResponses
                accessDeniedHandler = errorResponses
            }
            // By default, Spring saves a refused request in the session to replay it after the login. An API never
            // replays one, and saving it would open a session for every anonymous caller it refuses.
            requestCache { requestCache = NullRequestCache() }
            with(
                EmailCodeSignInConfigurer(
                    EmailCodeSignInFilter(jsonMapper, properties.code.maxEmailLength),
                    errorResponses,
                ),
            )
            logout {
                logoutUrl = LOGOUT_PATH
                logoutSuccessHandler = HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)
                addLogoutHandler(
                    CookieClearingLogoutHandler(
                        properties.cookie.forSite(properties.cookie.session, null, 0),
                        properties.cookie.forSite(properties.cookie.rememberMe, null, 0),
                    ),
                )
                // Cookies are expired one by one above: Chrome applies the "cookies" directive to every subdomain,
                // which would sign the user out of the sibling applications
                addLogoutHandler(
                    HeaderWriterLogoutHandler(ClearSiteDataHeaderWriter(Directive.CACHE, Directive.STORAGE)),
                )
            }
            headers {
                contentSecurityPolicy { policyDirectives = "default-src 'none'; frame-ancestors 'none'" }
                // On every request, not only the HTTPS ones: TLS may end at a proxy in front of the application,
                // which then receives plain HTTP requests.
                httpStrictTransportSecurity { requestMatcher = AnyRequestMatcher.INSTANCE }
                referrerPolicy { policy = ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN }
                permissionsPolicy { policy = "camera=(), microphone=(), geolocation=()" }
                crossOriginOpenerPolicy { policy = CrossOriginOpenerPolicy.SAME_ORIGIN }
                crossOriginResourcePolicy { policy = CrossOriginResourcePolicy.SAME_ORIGIN }
            }
            addFilterBefore<CsrfFilter>(FetchMetadataFilter(errorResponses))
            authorizeHttpRequests {
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ERROR), permitAll)
                authorize(HttpMethod.POST, CODE_REQUEST_PATH, permitAll)
                authorize(HttpMethod.POST, CODE_SIGN_IN_PATH, permitAll)
                authorize(HttpMethod.POST, LOGOUT_PATH, permitAll)
                authorize(HttpMethod.GET, CSRF_PATH, permitAll)
                if (ClassUtils.isPresent(ACTUATOR_ENDPOINTS, context.classLoader)) {
                    authorize(EndpointRequest.to("health"), permitAll)
                }
                endpointRules.orderedStream().forEach { it.declare(this) }
                authorize(anyRequest, UnruledEndpoint)
            }
        }

        return http.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun csrfTokenRepository(properties: SecurityStarterProperties): CsrfTokenRepository =
        CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
            setCookieName(properties.cookie.xsrfToken)
            setCookiePath("/")
            setCookieCustomizer { it.secure(properties.cookie.secure).sameSite(SAME_SITE) }
        }

    @Bean
    @ConditionalOnMissingBean
    fun sessionAuthenticationStrategy(csrfTokenRepository: CsrfTokenRepository): SessionAuthenticationStrategy {
        // Without an attribute name of its own, the handler reads the token's: that loads the new token, and writes
        // its cookie in the response of the sign in, which the next unsafe request can send without asking for one
        val newCsrfTokenAtOnce = CsrfTokenRequestAttributeHandler().apply { setCsrfRequestAttributeName(null) }
        val csrf = CsrfAuthenticationStrategy(csrfTokenRepository).apply { setRequestHandler(newCsrfTokenAtOnce) }
        return CompositeSessionAuthenticationStrategy(listOf(ChangeSessionIdAuthenticationStrategy(), csrf))
    }

    @Bean
    @ConditionalOnMissingBean
    fun securityContextRepository(): SecurityContextRepository = DelegatingSecurityContextRepository(
        RequestAttributeSecurityContextRepository(),
        HttpSessionSecurityContextRepository(),
    )

    @Bean
    @ConditionalOnMissingBean(name = ["csrfTokenEndpoint"])
    fun csrfTokenEndpoint(): RouterFunction<ServerResponse> = RouterFunctions.route()
        .GET(CSRF_PATH) { request ->
            // Spring Security creates the token lazily: reading it is what generates it and writes the XSRF cookie
            (request.servletRequest().getAttribute(CsrfToken::class.java.name) as CsrfToken?)?.token
            ServerResponse.noContent().build()
        }
        .build()

    @Bean
    @ConditionalOnMissingBean
    fun securityExceptionsResolver(): SecurityExceptionsResolver = SecurityExceptionsResolver()

    @Bean
    @ConditionalOnMissingBean
    fun securityEventLogger(): SecurityEventLogger = SecurityEventLogger()

    @Bean
    @ConditionalOnMissingBean
    fun authorizationEventPublisher(context: ApplicationContext): AuthorizationEventPublisher =
        SpringAuthorizationEventPublisher(context)
}
