package com.drinkit.security.infra.session

import com.drinkit.security.CurrentUser
import com.drinkit.security.SessionData
import com.drinkit.security.Sessions
import com.drinkit.security.infra.SecurityStarterProperties
import com.drinkit.security.infra.SecurityStarterProperties.Cookie.Companion.SAME_SITE
import com.drinkit.security.infra.events.SecurityEventLogger
import com.drinkit.security.infra.rememberme.RememberMeTokens
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.session.autoconfigure.DefaultCookieSerializerCustomizer
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.ConversionService
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import tools.jackson.databind.json.JsonMapper
import javax.sql.DataSource

@AutoConfiguration(after = [JdbcSessionAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "starters.security", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SecurityStarterProperties::class)
internal class SessionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(FindByIndexNameSessionRepository::class)
    @ConditionalOnSingleCandidate(DataSource::class)
    fun sessions(
        repository: FindByIndexNameSessionRepository<*>,
        rememberMeTokens: RememberMeTokens,
        events: SecurityEventLogger,
    ): Sessions = SpringSessions(repository, rememberMeTokens, events)

    @Bean
    @ConditionalOnMissingBean
    fun sessionData(currentUser: CurrentUser, jsonMapper: JsonMapper): SessionData =
        HttpSessionData(currentUser, jsonMapper)

    // Conditions on the bean name, not on the type: Spring Boot declares its own beans of these types, so a condition
    // on the type would always find one, and the starter would never register its own.
    @Bean
    @ConditionalOnMissingBean(name = ["securityStarterSessionRepository"])
    fun securityStarterSessionRepository(
        properties: SecurityStarterProperties,
    ): SessionRepositoryCustomizer<JdbcIndexedSessionRepository> = JdbcSessionCustomizer(properties.session)

    @Bean
    @ConditionalOnMissingBean(name = ["springSessionConversionService"])
    fun springSessionConversionService(context: ApplicationContext, events: SecurityEventLogger): ConversionService =
        JsonSessionConverter(context.classLoader ?: javaClass.classLoader, events)

    @Bean
    @ConditionalOnMissingBean(name = ["securityStarterSessionCookie"])
    fun securityStarterSessionCookie(properties: SecurityStarterProperties) =
        DefaultCookieSerializerCustomizer { serializer ->
            serializer.setCookieName(properties.cookie.session)
            serializer.setCookiePath("/")
            serializer.setUseSecureCookie(properties.cookie.secure)
            serializer.setUseHttpOnlyCookie(true)
            serializer.setSameSite(SAME_SITE)
        }
}
