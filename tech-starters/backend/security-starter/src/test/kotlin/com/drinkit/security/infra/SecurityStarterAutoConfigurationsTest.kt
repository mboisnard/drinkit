package com.drinkit.security.infra

import com.drinkit.security.CurrentUser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SessionData
import com.drinkit.security.SessionKey
import com.drinkit.security.Sessions
import com.drinkit.security.SignInCodeSender
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.UserAccounts
import com.drinkit.security.infra.chain.FetchMetadataFilter
import com.drinkit.security.infra.chain.SecurityAutoConfiguration
import com.drinkit.security.infra.code.EmailCodeSignInFilter
import com.drinkit.security.infra.code.InMemorySignInCodes
import com.drinkit.security.infra.code.JooqSignInCodes
import com.drinkit.security.infra.code.SignInCodeAutoConfiguration
import com.drinkit.security.infra.code.SignInCodes
import com.drinkit.security.infra.login.SpringCurrentUser
import com.drinkit.security.infra.login.UserAccountsAutoConfiguration
import com.drinkit.security.infra.rememberme.RememberMeAutoConfiguration
import com.drinkit.security.infra.rememberme.RememberMeTokens
import com.drinkit.security.infra.rememberme.RotatingRememberMeServices
import com.drinkit.security.infra.session.HttpSessionData
import com.drinkit.security.infra.session.InMemoryIndexedSessions
import com.drinkit.security.infra.session.JdbcSessionCustomizer
import com.drinkit.security.infra.session.JsonSessionConverter
import com.drinkit.security.infra.session.SessionsAutoConfiguration
import com.drinkit.security.infra.session.SpringSessions
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.session.autoconfigure.DefaultCookieSerializerCustomizer
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.NullRememberMeServices
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import javax.sql.DataSource
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration as BootSecurityAutoConfiguration

internal class SecurityStarterAutoConfigurationsTest {

    private val baseRunner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                BootSecurityAutoConfiguration::class.java,
                ServletWebSecurityAutoConfiguration::class.java,
                UserDetailsServiceAutoConfiguration::class.java,
                SessionAutoConfiguration::class.java,
                JooqAutoConfiguration::class.java,
                SecurityAutoConfiguration::class.java,
                SessionsAutoConfiguration::class.java,
                RememberMeAutoConfiguration::class.java,
                UserAccountsAutoConfiguration::class.java,
                SignInCodeAutoConfiguration::class.java,
            ),
        )
        .withBean(UserAccounts::class.java, { InMemoryUserAccounts() })
        .withBean(SignInCodeSender::class.java, { SpySignInCodeSender() })
        .withBean(Clock::class.java, { Clock.systemUTC() })

    // Without PostgreSQL, the application gives the codes itself, as the tests do
    private val runner = baseRunner.withBean(SignInCodes::class.java, { InMemorySignInCodes() })

    @Test
    fun `secures the application with its own filter chain, sign-in by code, current user and sessions`() {
        runner.run { context ->
            context.getBean<SecurityFilterChain>().filters.map { it::class }.shouldContainAll(
                EmailCodeSignInFilter::class,
                FetchMetadataFilter::class,
            )
            context.getBean<CurrentUser>().shouldBeInstanceOf<SpringCurrentUser>()
            context.getBean("springSessionConversionService").shouldBeInstanceOf<JsonSessionConverter>()
            context.getBean("securityStarterSessionRepository").shouldBeInstanceOf<JdbcSessionCustomizer>()
            context.getBean<SessionData>().shouldBeInstanceOf<HttpSessionData>()
            context.getBeansOfType(DefaultCookieSerializerCustomizer::class.java).keys shouldContainAll
                listOf("securityStarterSessionCookie", "rememberMeServicesCookieSerializerCustomizer")
        }
    }

    @Test
    fun `leaves no user of Spring Boot with a generated password`() {
        runner.run { context ->
            context.getBeanNamesForType(UserDetailsService::class.java).toList().shouldBeEmpty()
        }
    }

    @Test
    fun `remembers logins and revokes sessions when the sessions live in a database`() {
        withSessionsInDatabase.run { context ->
            context.getBean<RememberMeServices>().shouldBeInstanceOf<RotatingRememberMeServices>()
            context.getBean<SecurityFilterChain>().filters.map { it::class } shouldContain
                RememberMeAuthenticationFilter::class
            context.getBean<Sessions>().shouldBeInstanceOf<SpringSessions>()
            context.getBean<SignInCodes>().shouldBeInstanceOf<JooqSignInCodes>()
        }
    }

    @Test
    fun `remembers no login without a database`() {
        runner.run { context ->
            context.getBeanNamesForType(RememberMeServices::class.java).toList().shouldBeEmpty()
            context.getBeanNamesForType(RememberMeTokens::class.java).toList().shouldBeEmpty()
        }
    }

    @Test
    fun `backs off from each bean the application declares itself`() {
        val ownChain = DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE)
        val ownCurrentUser = object : CurrentUser by SpringCurrentUserStub {}
        val ownSessionData = object : SessionData {
            override fun <T : Any> get(key: SessionKey<T>): T? = null
            override fun <T : Any> set(key: SessionKey<T>, value: T) = Unit
            override fun remove(key: SessionKey<*>) = Unit
        }
        val ownConversion = DefaultConversionService()
        val ownRememberMe = NullRememberMeServices()

        withSessionsInDatabase
            .withBean(SecurityFilterChain::class.java, { ownChain })
            .withBean(CurrentUser::class.java, { ownCurrentUser })
            .withBean(SessionData::class.java, { ownSessionData })
            .withBean("springSessionConversionService", ConversionService::class.java, { ownConversion })
            .withBean(RememberMeServices::class.java, { ownRememberMe })
            .run { context ->
                context.getBean<SecurityFilterChain>() shouldBeSameInstanceAs ownChain
                context.getBean<CurrentUser>() shouldBeSameInstanceAs ownCurrentUser
                context.getBean<SessionData>() shouldBeSameInstanceAs ownSessionData
                context.getBean("springSessionConversionService") shouldBeSameInstanceAs ownConversion
                context.getBean<RememberMeServices>() shouldBeSameInstanceAs ownRememberMe
            }
    }

    @Test
    fun `registers nothing when disabled`() {
        withSessionsInDatabase.withPropertyValues("starters.security.enabled=false")
            .run { context ->
                context.getBeanNamesForType(CurrentUser::class.java).toList().shouldBeEmpty()
                context.getBeanNamesForType(SignInCodes::class.java).toList().shouldBeEmpty()
                context.containsBean("signInCodeEndpoint") shouldBe false
                context.getBeanNamesForType(SessionData::class.java).toList().shouldBeEmpty()
                context.containsBean("springSessionConversionService") shouldBe false
                context.containsBean("securityStarterSessionRepository") shouldBe false
                context.getBeanNamesForType(Sessions::class.java).toList().shouldBeEmpty()
                context.getBeanNamesForType(RememberMeServices::class.java).toList().shouldBeEmpty()
            }
    }

    @Test
    fun `refuses an unsecure cookie with a prefix`() {
        runner.withPropertyValues("starters.security.cookie.secure=false")
            .run { context ->
                context.startupFailure?.rootCause()?.message.orEmpty() shouldContain
                    "starters.security.cookie.prefix must be empty when starters.security.cookie.secure is false"
            }
    }

    @Test
    fun `names the XSRF cookie without prefix nor Secure flag, for plain http in development`() {
        runner.withPropertyValues("starters.security.cookie.prefix=", "starters.security.cookie.secure=false")
            .run { context ->
                val request = MockHttpServletRequest()
                val response = MockHttpServletResponse()
                val repository = context.getBean<CsrfTokenRepository>()
                repository.saveToken(repository.generateToken(request), request, response)

                response.getCookie("XSRF-TOKEN").shouldNotBeNull().secure shouldBe false
            }
    }

    // Never connected to: the beans only keep the data source
    private val dataSource = DriverManagerDataSource("jdbc:postgresql://localhost:1/none")
    private val withSessionsInDatabase = baseRunner
        .withBean(FindByIndexNameSessionRepository::class.java, { InMemoryIndexedSessions() })
        .withBean(DataSource::class.java, { dataSource })
        .withBean(PlatformTransactionManager::class.java, { DataSourceTransactionManager(dataSource) })

    private fun Throwable.rootCause(): Throwable = cause?.rootCause() ?: this

    private object SpringCurrentUserStub : CurrentUser {
        override fun get() = error("stub")
        override fun orNull() = null
        override fun signIn(userId: String) = error("stub")
        override fun refresh() = null
    }
}
